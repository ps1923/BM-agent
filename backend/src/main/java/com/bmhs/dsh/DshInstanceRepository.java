package com.bmhs.dsh;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DshInstanceRepository {
    private final JdbcTemplate jdbc;

    public DshInstanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DshInstanceModels.Instance> findByStudent(long studentId) {
        List<DshInstanceModels.Instance> rows = jdbc.query("""
                SELECT id, student_id, route_key, container_id, storage_volume, proxy_port, status,
                       cpu_limit, memory_limit_mb, pids_limit, last_active_at
                FROM dsh_instances WHERE student_id = ?
                """, (rs, rowNum) -> new DshInstanceModels.Instance(
                rs.getLong("id"), rs.getLong("student_id"), rs.getString("route_key"),
                rs.getString("container_id"), rs.getString("storage_volume"),
                rs.getObject("proxy_port") == null ? null : ((Number) rs.getObject("proxy_port")).intValue(), rs.getString("status"),
                rs.getBigDecimal("cpu_limit").doubleValue(), rs.getInt("memory_limit_mb"),
                rs.getInt("pids_limit"), rs.getTimestamp("last_active_at") == null ? null
                : rs.getTimestamp("last_active_at").toInstant()), studentId);
        return rows.stream().findFirst();
    }

    public DshInstanceModels.Instance getOrCreate(long studentId, String routeKey, String volumeName,
                                                   double cpuLimit, int memoryLimitMb, int pidsLimit) {
        jdbc.update("""
                INSERT INTO dsh_instances (student_id, route_key, storage_volume, status, cpu_limit, memory_limit_mb, pids_limit)
                VALUES (?, ?, ?, 'stopped', ?, ?, ?)
                ON DUPLICATE KEY UPDATE student_id = VALUES(student_id)
                """, studentId, routeKey, volumeName, cpuLimit, memoryLimitMb, pidsLimit);
        return findByStudent(studentId).orElseThrow(() -> new IllegalStateException("DSH 实例记录创建失败"));
    }

    public boolean claimStart(long instanceId) {
        return jdbc.update("""
                UPDATE dsh_instances SET status = 'provisioning', container_id = NULL, proxy_port = NULL
                WHERE id = ? AND status IN ('stopped', 'error')
                """, instanceId) == 1;
    }

    public boolean markRunning(long instanceId, String containerId, int proxyPort) {
        return jdbc.update("""
                UPDATE dsh_instances SET status = 'running', container_id = ?, proxy_port = ?,
                    last_active_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND status = 'provisioning'
                """, containerId, proxyPort, instanceId) == 1;
    }

    public void markError(long instanceId) {
        jdbc.update("""
                UPDATE dsh_instances SET status = 'error', container_id = NULL, proxy_port = NULL
                WHERE id = ? AND status IN ('provisioning', 'stopping', 'running')
                """, instanceId);
    }

    public boolean claimStop(long instanceId, Instant idleBefore) {
        return jdbc.update("""
                UPDATE dsh_instances SET status = 'stopping'
                WHERE id = ? AND status = 'running' AND last_active_at < ?
                """, instanceId, Timestamp.from(idleBefore)) == 1;
    }

    public boolean claimStopNow(long instanceId, long studentId) {
        return jdbc.update("UPDATE dsh_instances SET status = 'stopping' WHERE id = ? AND student_id = ? AND status = 'running'",
                instanceId, studentId) == 1;
    }

    public List<DshInstanceModels.Instance> findIdle(Instant idleBefore, int limit) {
        return jdbc.query("""
                SELECT id, student_id, route_key, container_id, storage_volume, proxy_port, status,
                       cpu_limit, memory_limit_mb, pids_limit, last_active_at
                FROM dsh_instances WHERE status = 'running' AND last_active_at < ?
                ORDER BY last_active_at ASC LIMIT ?
                """, (rs, rowNum) -> new DshInstanceModels.Instance(
                rs.getLong("id"), rs.getLong("student_id"), rs.getString("route_key"),
                rs.getString("container_id"), rs.getString("storage_volume"),
                rs.getObject("proxy_port") == null ? null : ((Number) rs.getObject("proxy_port")).intValue(), rs.getString("status"),
                rs.getBigDecimal("cpu_limit").doubleValue(), rs.getInt("memory_limit_mb"),
                rs.getInt("pids_limit"), rs.getTimestamp("last_active_at").toInstant()),
                Timestamp.from(idleBefore), limit);
    }

    public void markStopped(long instanceId) {
        jdbc.update("""
                UPDATE dsh_instances SET status = 'stopped', container_id = NULL, proxy_port = NULL
                WHERE id = ? AND status = 'stopping'
                """, instanceId);
    }

    public Optional<Integer> proxyPort(String routeKey, long studentId) {
        List<Integer> ports = jdbc.query("""
                SELECT proxy_port FROM dsh_instances
                WHERE route_key = ? AND student_id = ? AND status = 'running' AND proxy_port IS NOT NULL
                """, (rs, rowNum) -> rs.getInt(1), routeKey, studentId);
        return ports.stream().findFirst();
    }

    public void markActive(long instanceId) {
        jdbc.update("UPDATE dsh_instances SET last_active_at = CURRENT_TIMESTAMP(6) WHERE id = ? AND status = 'running'", instanceId);
    }
}
