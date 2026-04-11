package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.common.minio.service.MinioService;
import com.maidc.model.entity.ModelEntity;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.ModelRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.VersionCompareVO;
import com.maidc.model.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final ModelRepository modelRepository;
    private final MinioService minioService;
    private final ModelMapper modelMapper;

    private static final String MODELS_BUCKET = "maidc-models";

    @Transactional
    public VersionVO createVersion(Long modelId, String versionNo, String description,
                                    String changelog, String hyperParamsJson,
                                    MultipartFile modelFile, MultipartFile configFile) {
        ModelEntity model = modelRepository.findByIdAndIsDeletedFalse(modelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        if (versionRepository.existsByModelIdAndVersionNoAndIsDeletedFalse(modelId, versionNo)) {
            throw new BusinessException(ErrorCode.VERSION_NO_DUPLICATE);
        }

        ModelVersionEntity version = new ModelVersionEntity();
        version.setModelId(modelId);
        version.setVersionNo(versionNo);
        version.setDescription(description);
        version.setChangelog(changelog);
        version.setStatus("CREATED");

        try {
            // Upload model file to MinIO
            String objectPath = model.getOrgId() + "/" + model.getModelCode() + "/" + versionNo + "/model.pt";
            String checksum = calculateChecksum(modelFile.getInputStream());

            minioService.uploadFile(MODELS_BUCKET, objectPath,
                    modelFile.getInputStream(), modelFile.getContentType());

            version.setModelFilePath(objectPath);
            version.setModelFileSize(modelFile.getSize());
            version.setModelFileChecksum(checksum);

            // Upload config file if present
            if (configFile != null && !configFile.isEmpty()) {
                String configPath = model.getOrgId() + "/" + model.getModelCode() + "/" + versionNo + "/config.yaml";
                minioService.uploadFile(MODELS_BUCKET, configPath,
                        configFile.getInputStream(), configFile.getContentType());
                version.setConfigPath(configPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("模型文件上传失败: " + e.getMessage(), e);
        }

        version = versionRepository.save(version);
        log.info("模型版本创建成功: modelId={}, version={}", modelId, versionNo);
        return modelMapper.toVersionVO(version);
    }

    public PageResult<VersionVO> listVersions(Long modelId, int page, int pageSize) {
        Page<ModelVersionEntity> result = versionRepository.findByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                modelId, PageRequest.of(page - 1, pageSize));
        return PageResult.of(result.map(modelMapper::toVersionVO));
    }

    public VersionVO getVersionDetail(Long modelId, Long versionId) {
        ModelVersionEntity version = versionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
        if (!version.getModelId().equals(modelId)) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        return modelMapper.toVersionVO(version);
    }

    public VersionCompareVO compareVersions(Long modelId, Long v1Id, Long v2Id) {
        ModelVersionEntity v1 = versionRepository.findByIdAndIsDeletedFalse(v1Id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
        ModelVersionEntity v2 = versionRepository.findByIdAndIsDeletedFalse(v2Id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

        if (!v1.getModelId().equals(modelId) || !v2.getModelId().equals(modelId)) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }

        Map<String, Object> diff = new HashMap<>();
        diff.put("version_no_delta", v2.getVersionNo() + " vs " + v1.getVersionNo());
        diff.put("file_size_delta", v2.getModelFileSize() - v1.getModelFileSize());

        if (v1.getTrainingMetrics() != null && v2.getTrainingMetrics() != null) {
            Map<String, Object> metricsDiff = new HashMap<>();
            v1.getTrainingMetrics().fieldNames().forEachRemaining(field -> {
                if (v2.getTrainingMetrics().has(field)) {
                    double val1 = v1.getTrainingMetrics().get(field).asDouble();
                    double val2 = v2.getTrainingMetrics().get(field).asDouble();
                    metricsDiff.put(field, val2 - val1);
                }
            });
            diff.put("metrics_diff", metricsDiff);
        }

        if (v1.getHyperParams() != null && v2.getHyperParams() != null) {
            Map<String, Object> hyperDiff = new HashMap<>();
            v1.getHyperParams().fieldNames().forEachRemaining(field -> {
                if (v2.getHyperParams().has(field)) {
                    if (!v1.getHyperParams().get(field).equals(v2.getHyperParams().get(field))) {
                        hyperDiff.put(field, Map.of("v1", v1.getHyperParams().get(field), "v2", v2.getHyperParams().get(field)));
                    }
                }
            });
            diff.put("hyperparams_diff", hyperDiff);
        }

        return VersionCompareVO.builder()
                .v1(modelMapper.toVersionVO(v1))
                .v2(modelMapper.toVersionVO(v2))
                .diff(diff)
                .build();
    }

    public ResponseEntity<Resource> downloadVersion(Long modelId, Long versionId) {
        ModelVersionEntity version = versionRepository.findByIdAndIsDeletedFalse(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
        if (!version.getModelId().equals(modelId)) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }

        InputStream inputStream = minioService.downloadFile(MODELS_BUCKET, version.getModelFilePath());
        String filename = version.getModelFilePath().substring(version.getModelFilePath().lastIndexOf('/') + 1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(version.getModelFileSize())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    private String calculateChecksum(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return "sha256:" + hexString;
    }
}
