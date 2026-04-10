package com.maidc.data.repository;

import com.maidc.data.entity.ProjectMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, Long> {

    List<ProjectMemberEntity> findByProjectIdAndIsDeletedFalse(Long projectId);

    Optional<ProjectMemberEntity> findByProjectIdAndUserIdAndIsDeletedFalse(Long projectId, Long userId);
}
