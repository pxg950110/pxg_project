package com.maidc.data.service.followup;

import com.maidc.data.entity.FollowupTaskEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.mq.FollowupNotifyProducer;
import com.maidc.data.repository.PatientFollowupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 随访提醒定时任务（设计 §4 提醒分级）：
 * - 当日到期 / 已超期 PENDING 任务 → 站内信负责护士（无护士则医生）
 * - 超 7 天未办 → 追加升级通知负责医生
 * 按 (taskId, 日期) 内存去重，每日至多提醒一次；多实例部署需换 Redis 去重。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowupReminderScheduler {

    static final int ESCALATE_AFTER_DAYS = 7;

    private final PatientFollowupRepository followupRepository;
    private final FollowupTaskService taskService;
    private final FollowupNotifyProducer notifyProducer;

    /** 去重键: taskId@yyyy-MM-dd */
    private final Set<String> sentToday = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Shanghai")
    public void dailyRemind() {
        int sent = runReminders(LocalDate.now());
        log.info("随访提醒扫描完成: 发送 {} 条", sent);
    }

    /** 扫描并提醒，返回发送条数（供定时与手动触发共用） */
    public int runReminders(LocalDate today) {
        // 全量扫 ACTIVE 档案（量级 = 在管患者数）
        List<PatientFollowupEntity> actives = followupRepository.findAll().stream()
                .filter(f -> Boolean.FALSE.equals(f.getIsDeleted()) && "ACTIVE".equals(f.getStatus()))
                .toList();
        if (actives.isEmpty()) return 0;

        int sent = 0;
        for (PatientFollowupEntity f : actives) {
            for (FollowupTaskEntity t : taskService.pendingTasks(f.getId())) {
                boolean dueToday = t.getDueDate().isEqual(today);
                boolean overdue = t.getDueDate().isBefore(today);
                if (!dueToday && !overdue) continue;

                long overdueDays = overdue ? today.toEpochDay() - t.getDueDate().toEpochDay() : 0;
                Long nurseReceiver = f.getNurseId() != null ? f.getNurseId() : f.getDoctorId();
                try {
                    sent += remind(f, t, nurseReceiver, dueToday, overdueDays, today, false);
                } catch (Exception e) {
                    log.warn("随访提醒发送失败 taskId={}: {}", t.getId(), e.getMessage());
                }

                if (overdueDays >= ESCALATE_AFTER_DAYS) {
                    try {
                        sent += remind(f, t, f.getDoctorId(), false, overdueDays, today, true);
                    } catch (Exception e) {
                        log.warn("随访升级提醒发送失败 taskId={}: {}", t.getId(), e.getMessage());
                    }
                }
            }
        }
        return sent;
    }

    private int remind(PatientFollowupEntity f, FollowupTaskEntity t, Long receiver,
                       boolean dueToday, long overdueDays, LocalDate today, boolean escalate) {
        String dedupeKey = t.getId() + "@" + today.format(DateTimeFormatter.ISO_DATE) + (escalate ? "#esc" : "");
        if (!sentToday.add(dedupeKey)) return 0;

        String stage = t.getStageName() == null ? t.getStageCode() : t.getStageName();
        String title;
        String content;
        if (escalate) {
            title = "随访超期升级：任务已超期 " + overdueDays + " 天";
            content = String.format("患者档案 #%d 的「%s」任务（应办 %s）已超期 %d 天未完成，请跟进处理。",
                    f.getId(), stage, t.getDueDate(), overdueDays);
        } else if (dueToday) {
            title = "今日随访任务待办";
            content = String.format("患者档案 #%d 的「%s」任务今日到期，请及时完成随访评估。", f.getId(), stage);
        } else {
            title = "随访任务已超期 " + overdueDays + " 天";
            content = String.format("患者档案 #%d 的「%s」任务（应办 %s）已超期，请尽快处理。", f.getId(), stage, t.getDueDate());
        }

        try {
            notifyProducer.sendFollowupNotify(escalate ? "followup.escalate" : (dueToday ? "followup.due" : "followup.overdue"),
                    Map.of("userId", receiver,
                            "title", title,
                            "content", content,
                            "messageType", "FOLLOWUP",
                            "bizId", t.getId(),
                            "bizType", "FOLLOWUP_TASK",
                            "followupId", f.getId(),
                            "stage", stage));
            return 1;
        } catch (Exception e) {
            sentToday.remove(dedupeKey); // 发送失败回滚去重位，下次扫描重试
            throw e;
        }
    }
}
