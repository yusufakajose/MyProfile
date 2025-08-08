package com.linkgrove.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.indexes.enabled:true}")
    private boolean indexesEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!indexesEnabled) {
            log.info("IndexInitializer disabled via app.indexes.enabled=false");
            return;
        }

        List<String> statements = List.of(
                // Links
                "CREATE INDEX IF NOT EXISTS idx_links_user_display_order ON links(user_id, display_order)",
                "CREATE INDEX IF NOT EXISTS idx_links_user ON links(user_id)",

                // Tags join table
                "CREATE INDEX IF NOT EXISTS idx_link_tags_link ON link_tags(link_id)",
                "CREATE INDEX IF NOT EXISTS idx_link_tags_tag ON link_tags(tag_id)",

                // Daily aggregates
                "CREATE INDEX IF NOT EXISTS idx_click_user_day ON link_click_daily_aggregate(username, day)",
                "CREATE INDEX IF NOT EXISTS idx_ref_user_day ON link_referrer_daily_aggregate(username, day)",
                "CREATE INDEX IF NOT EXISTS idx_dev_user_day ON link_device_daily_aggregate(username, day)"
        );

        for (String sql : statements) {
            try {
                jdbcTemplate.execute(sql);
                log.info("Ensured index: {}", sql);
            } catch (Exception e) {
                log.warn("Failed to ensure index for statement: {} - {}", sql, e.getMessage());
            }
        }
    }
}


