package com.sp.api.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlywayRepairConfigTest {

    private final FlywayMigrationStrategy strategy = new FlywayRepairConfig().repairFailedThenMigrate();

    private Flyway flywayWith(MigrationState... states) {
        Flyway flyway = mock(Flyway.class);
        MigrationInfoService service = mock(MigrationInfoService.class);

        MigrationInfo[] infos = new MigrationInfo[states.length];
        for (int i = 0; i < states.length; i++) {
            MigrationInfo info = mock(MigrationInfo.class);
            when(info.getState()).thenReturn(states[i]);
            when(info.getVersion()).thenReturn(org.flywaydb.core.api.MigrationVersion.fromVersion(String.valueOf(i + 1)));
            infos[i] = info;
        }

        when(service.all()).thenReturn(infos);
        when(flyway.info()).thenReturn(service);

        return flyway;
    }

    @Test
    @DisplayName("실패한 기록이 있으면 먼저 정리(repair)하고 그다음 마이그레이션한다")
    void repairsFailedThenMigrates() {

        Flyway flyway = flywayWith(MigrationState.SUCCESS, MigrationState.FAILED);

        strategy.migrate(flyway);

        InOrder order = inOrder(flyway);
        order.verify(flyway).repair();
        order.verify(flyway).migrate();
    }

    @Test
    @DisplayName("실패한 기록이 없으면 repair 없이 마이그레이션만 한다")
    void justMigratesWhenNothingFailed() {

        Flyway flyway = flywayWith(MigrationState.SUCCESS, MigrationState.PENDING);

        strategy.migrate(flyway);

        verify(flyway, never()).repair();
        verify(flyway).migrate();
    }
}
