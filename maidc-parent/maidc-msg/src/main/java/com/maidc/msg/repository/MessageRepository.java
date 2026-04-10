package com.maidc.msg.repository;

import com.maidc.msg.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Page<MessageEntity> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<MessageEntity> findByUserIdAndTypeAndIsDeletedFalse(Long userId, String type, Pageable pageable);

    Page<MessageEntity> findByUserIdAndIsReadAndIsDeletedFalse(Long userId, Boolean isRead, Pageable pageable);

    Page<MessageEntity> findByUserIdAndTypeAndIsReadAndIsDeletedFalse(Long userId, String type, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    long countByUserIdAndTypeAndIsReadFalseAndIsDeletedFalse(Long userId, String type);

    List<MessageEntity> findByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    @Modifying
    @Query("UPDATE MessageEntity m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.userId = :userId AND m.isRead = false AND m.isDeleted = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM MessageEntity m WHERE m.userId = :userId AND m.isDeleted = false " +
            "AND (:type IS NULL OR m.type = :type) " +
            "AND (:isRead IS NULL OR m.isRead = :isRead) " +
            "ORDER BY m.createdAt DESC")
    Page<MessageEntity> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Boolean isRead,
            Pageable pageable);
}
