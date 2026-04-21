package com.maidc.task.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkspaceMetricsRepository {

    @PersistenceContext
    private EntityManager em;

    public long countModelByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_model WHERE org_id = :orgId AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countActiveDeploymentsByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_deployment WHERE org_id = :orgId AND status = 'RUNNING' AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countTodayInferencesByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(invocation_count), 0) FROM model.m_model_daily_stats WHERE org_id = :orgId AND stat_date = CURRENT_DATE")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    public long countPendingApprovalsByOrgId(long orgId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM model.m_approval WHERE org_id = :orgId AND status = 'PENDING' AND is_deleted = false")
                .setParameter("orgId", orgId).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findRecentMessagesByUserId(Long userId, int limit) {
        return em.createNativeQuery(
                "SELECT id, type, title, content, is_read, created_at " +
                "FROM model.m_message " +
                "WHERE user_id = :userId AND is_deleted = false " +
                "ORDER BY is_read ASC, created_at DESC")
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
    }
}
