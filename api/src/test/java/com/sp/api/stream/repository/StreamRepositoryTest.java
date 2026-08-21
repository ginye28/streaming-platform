package com.sp.api.stream.repository;

import com.sp.api.stream.entity.Stream;
import com.sp.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StreamRepositoryTest {

    @Autowired
    private StreamRepository streamRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("조회수는 DB 에서 직접 증가한다")
    void increaseViewCount() {

        User user = em.persistAndFlush(new User("v@test.com", "encoded", "v"));
        Stream stream = em.persistAndFlush(
                new Stream("제목", "설명", null, "/uploads/a.mp4", user)
        );
        em.clear();

        assertThat(streamRepository.increaseViewCount(stream.getId())).isEqualTo(1);
        assertThat(streamRepository.increaseViewCount(stream.getId())).isEqualTo(1);

        Stream reloaded = streamRepository.findWithUserById(stream.getId()).orElseThrow();

        assertThat(reloaded.getViewCount()).isEqualTo(2L);
        assertThat(reloaded.getUser().getNickname()).isEqualTo("v");
    }

    @Test
    @DisplayName("없는 영상의 조회수 증가는 0건을 반환한다")
    void increaseViewCount_missingStream() {
        assertThat(streamRepository.increaseViewCount(9999L)).isZero();
    }
}
