package com.maidc.msg.repository;

import com.maidc.msg.entity.MessageTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTemplateRepository extends JpaRepository<MessageTemplateEntity, Long> {

    Optional<MessageTemplateEntity> findByCodeAndIsDeletedFalse(String code);

    List<MessageTemplateEntity> findByIsDeletedFalse();

    List<MessageTemplateEntity> findByEventTypeAndIsDeletedFalse(String eventType);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
