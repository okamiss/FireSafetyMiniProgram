package com.firesafety.platform.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayBaselineTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsFoundationTables() {
        var tableNames = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where lower(table_schema) = 'public'",
                String.class);

        assertThat(tableNames).extracting(String::toLowerCase)
                .contains("enterprise", "sys_user", "file_resource", "operation_log");
    }
}
