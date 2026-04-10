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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

        Map<String, Object> diff = new HashMap<>();
        if (v1.getTrainingMetrics() != null && v2.getTrainingMetrics() != null) {
            diff.put("metrics_diff", "请在前端展示详细对比");
        }
        diff.put("file_size_delta", v2.getModelFileSize() - v1.getModelFileSize());

        return VersionCompareVO.builder()
                .v1(modelMapper.toVersionVO(v1))
                .v2(modelMapper.toVersionVO(v2))
                .diff(diff)
                .build();
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
