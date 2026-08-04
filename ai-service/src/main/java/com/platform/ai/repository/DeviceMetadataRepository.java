package com.platform.ai.repository;

import com.platform.ai.model.DeviceMetadata;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DeviceMetadataRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeviceMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DeviceMetadata> findBySn(String sn) {
        String sql = """
                SELECT d.id, d.device_id, d.sn, d.model_id, dm.model_name,
                       d.online_status, d.lifecycle_status, d.activated_at, d.created_at
                FROM device d
                LEFT JOIN device_model dm ON dm.id = d.model_id AND dm.deleted = 0
                WHERE d.sn = ? AND d.deleted = 0
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> DeviceMetadata.builder()
                    .id(rs.getLong("id"))
                    .deviceId(rs.getString("device_id"))
                    .sn(rs.getString("sn"))
                    .modelId(rs.getLong("model_id"))
                    .modelName(rs.getString("model_name"))
                    .online(rs.getInt("online_status") == 1)
                    .lifecycleStatus(rs.getString("lifecycle_status"))
                    .activatedAt(rs.getTimestamp("activated_at") == null ? null : rs.getTimestamp("activated_at").toLocalDateTime())
                    .createdAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime())
                    .build(), sn));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
