package com.sp.api.schema;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이그레이션 SQL 과 엔티티가 어긋나지 않았는지 진짜 MySQL 로 확인한다.
 *
 * <p>나머지 테스트는 H2 로 도는데, H2 는 MySQL 문법의 마이그레이션을 실행하지 못한다.
 * 그래서 엔티티에 칸을 하나 더해 놓고 마이그레이션을 안 쓰는 실수를 H2 테스트로는 잡을 수 없다.
 * 이 테스트가 그 구멍을 막는다.
 *
 * <p>확인하는 것은 두 가지다.
 * <ol>
 *   <li>빈 DB 에 db/migration 의 SQL 이 전부 적용되는가</li>
 *   <li>그렇게 만들어진 표가 엔티티와 맞는가 — {@code ddl-auto: validate} 라 안 맞으면 기동이 멈춘다</li>
 * </ol>
 *
 * <p>MySQL 주소를 환경변수로 주지 않으면 통째로 건너뛴다. 로컬에서 돌려 보려면:
 * <pre>
 * SCHEMA_TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/sp_schema_test' \
 * SCHEMA_TEST_DB_USERNAME=root SCHEMA_TEST_DB_PASSWORD= \
 *   ./gradlew test --tests '*SchemaMigrationTest'
 * </pre>
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SCHEMA_TEST_DB_URL", matches = ".+")
@TestPropertySource(properties = {
        "spring.datasource.url=${SCHEMA_TEST_DB_URL}",
        "spring.datasource.username=${SCHEMA_TEST_DB_USERNAME:root}",
        "spring.datasource.password=${SCHEMA_TEST_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        // 여기서는 빈 DB 로 시작하므로 도장만 찍고 넘어가는 일이 없어야 한다.
        // 베이스라인까지 실제로 실행되는지 보려고 일부러 꺼 둔다.
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.hibernate.ddl-auto=validate",
})
class SchemaMigrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("마이그레이션이 전부 적용되고, 그 스키마가 엔티티와 맞는다")
    void migrationsMatchEntities() {

        // 이 테스트가 여기까지 왔다는 것은 ddl-auto: validate 로 기동이 됐다는 뜻이다.
        // 엔티티에 있는 칸이 표에 없으면 컨텍스트가 뜨다가 멈춘다.

        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied)
                .describedAs("db/migration 의 SQL 이 하나도 적용되지 않았다")
                .isNotEmpty();

        assertThat(Arrays.stream(applied).map(info -> info.getVersion().getVersion()))
                .describedAs("베이스라인 V1 이 실행되지 않았다")
                .contains("1");

        assertThat(flyway.info().pending())
                .describedAs("적용되지 않고 남은 마이그레이션이 있다")
                .isEmpty();
    }
}
