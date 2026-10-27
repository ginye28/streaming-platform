package com.sp.api.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 마이그레이션이 중간에 실패하면 Flyway 가 "V8 실패" 기록을 남기고, 그 뒤로는 파일을 고쳐도
 * "Detected failed migration" 으로 기동 자체를 거부한다(무료 Render 는 재시작을 되풀이하며 멈춘다).
 *
 * 그래서 실패 기록이 있을 때만 그 기록을 정리(repair)하고 다시 마이그레이션한다.
 * repair 는 실패한 기록만 지우고 이미 성공한 마이그레이션은 건드리지 않는다.
 * 실패 기록이 없으면 평소처럼 migrate 만 한다. 실패의 원인(SQL)을 고치지 않았다면 다시 같은 곳에서 실패한다.
 */
@Slf4j
@Configuration
public class FlywayRepairConfig {

    @Bean
    FlywayMigrationStrategy repairFailedThenMigrate() {
        return flyway -> {
            MigrationInfo[] failed = Arrays.stream(flyway.info().all())
                    .filter(info -> info.getState().isFailed())
                    .toArray(MigrationInfo[]::new);

            if (failed.length > 0) {
                log.warn("실패한 마이그레이션 기록 {}건을 정리하고 다시 실행합니다: {}",
                        failed.length,
                        Arrays.stream(failed).map(info -> "V" + info.getVersion()).toList());
                flyway.repair();
            }

            flyway.migrate();
        };
    }
}
