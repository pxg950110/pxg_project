package com.maidc.data.repository;

import com.maidc.data.entity.ImportTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportTaskRepository extends JpaRepository<ImportTaskEntity, Long> {
}
