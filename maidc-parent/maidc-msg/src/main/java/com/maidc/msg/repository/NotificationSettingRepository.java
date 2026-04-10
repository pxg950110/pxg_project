package com.maidc.msg.repository;

import com.maidc.msg.entity.NotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSettingEntity, Long> {

    List<NotificationSettingEntity> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<NotificationSettingEntity> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    Optional<NotificationSettingEntity> findByUserIdAndChannelAndEventTypeAndIsDeletedFalse(Long userId, String channel, String eventType);
}
