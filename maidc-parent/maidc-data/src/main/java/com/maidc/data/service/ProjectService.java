package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.ProjectCreateDTO;
import com.maidc.data.dto.ProjectQueryDTO;
import com.maidc.data.entity.DatasetEntity;
import com.maidc.data.entity.ProjectEntity;
import com.maidc.data.entity.ProjectMemberEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.ProjectMemberRepository;
import com.maidc.data.repository.ProjectRepository;
import com.maidc.data.repository.ProjectSpecification;
import com.maidc.data.vo.ProjectDetailVO;
import com.maidc.data.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DatasetRepository datasetRepository;
    private final DataMapper dataMapper;

    @Transactional
    public ProjectVO createProject(ProjectCreateDTO dto) {
        ProjectEntity entity = new ProjectEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setResearchType(dto.getResearchType());
        entity.setPiId(dto.getPiId());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus("DRAFT");
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);

        entity = projectRepository.save(entity);
        log.info("科研项目创建成功: id={}, name={}", entity.getId(), entity.getName());
        return enrichProjectVO(entity);
    }

    public ProjectVO getProject(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return enrichProjectVO(entity);
    }

    public ProjectDetailVO getProjectDetail(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<ProjectDetailVO.ProjectMemberVO> members = projectMemberRepository
                .findByProjectIdAndIsDeletedFalse(id)
                .stream()
                .map(dataMapper::toProjectMemberVO)
                .toList();

        List<com.maidc.data.vo.DatasetVO> datasets = datasetRepository
                .findByProjectIdAndIsDeletedFalse(id)
                .stream()
                .map(dataMapper::toDatasetVO)
                .toList();

        return ProjectDetailVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .researchType(entity.getResearchType())
                .piId(entity.getPiId())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .members(members)
                .datasets(datasets)
                .memberCount(members.size())
                .datasetCount(datasets.size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PageResult<ProjectVO> listProjects(ProjectQueryDTO query) {
        Specification<ProjectEntity> spec = ProjectSpecification.buildSearchSpec(
                query.getOrgId(), query.getKeyword(), query.getResearchType(), query.getStatus());

        Page<ProjectEntity> page = projectRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize()));

        Page<ProjectVO> voPage = page.map(this::enrichProjectVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public void addMember(Long projectId, Long userId, String role) {
        projectRepository.findByIdAndIsDeletedFalse(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        projectMemberRepository.findByProjectIdAndUserIdAndIsDeletedFalse(projectId, userId)
                .ifPresent(m -> {
                    throw new BusinessException(409, "该成员已在项目中");
                });

        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(LocalDateTime.now());
        member.setOrgId(0L);
        projectMemberRepository.save(member);
        log.info("项目成员添加成功: projectId={}, userId={}, role={}", projectId, userId, role);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        ProjectMemberEntity member = projectMemberRepository
                .findByProjectIdAndUserIdAndIsDeletedFalse(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        projectMemberRepository.delete(member);
        log.info("项目成员移除成功: projectId={}, userId={}", projectId, userId);
    }

    @Transactional
    public void deleteProject(Long id) {
        ProjectEntity entity = projectRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        projectRepository.delete(entity);
        log.info("科研项目已删除: id={}", id);
    }

    private ProjectVO enrichProjectVO(ProjectEntity entity) {
        ProjectVO vo = dataMapper.toProjectVO(entity);
        int memberCount = projectMemberRepository.findByProjectIdAndIsDeletedFalse(entity.getId()).size();
        int datasetCount = datasetRepository.findByProjectIdAndIsDeletedFalse(entity.getId()).size();
        vo.setMemberCount(memberCount);
        vo.setDatasetCount(datasetCount);
        return vo;
    }
}
