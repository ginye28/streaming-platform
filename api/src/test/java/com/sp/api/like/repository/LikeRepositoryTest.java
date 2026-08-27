package com.sp.api.like.repository;

import com.sp.api.like.entity.Like;
import com.sp.api.stream.entity.Stream;
import com.sp.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private TestEntityManager em;

    /**
     * 파생 쿼리는 파라미터 '이름'이 아니라 메서드명 순서로 바인딩된다.
     * 인자 순서가 뒤바뀌면 userId 와 streamId 가 같은 경우에만 우연히 통과하므로,
     * 일부러 서로 다른 id 가 생기도록 데이터를 만든다.
     */
    @Test
    @DisplayName("streamId 와 userId 가 서로 다를 때도 좋아요를 찾는다")
    void findByStreamIdAndUserId_bindsArgumentsInMethodNameOrder() {

        // user 를 여러 개 만들어 user.id 와 stream.id 가 겹치지 않게 한다.
        persistUser("a@test.com", "a");
        persistUser("b@test.com", "b");
        User user = persistUser("c@test.com", "c");

        Stream stream = persistStream(user);

        assertThat(user.getId()).isNotEqualTo(stream.getId());

        em.persistAndFlush(new Like(user, stream));
        em.clear();

        Optional<Like> found =
                likeRepository.findByStreamIdAndUserId(stream.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(likeRepository.existsByStreamIdAndUserId(stream.getId(), user.getId())).isTrue();
        assertThat(likeRepository.countByStreamId(stream.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사용자의 좋아요는 조회되지 않는다")
    void findByStreamIdAndUserId_doesNotMatchOtherUser() {

        User owner = persistUser("owner@test.com", "owner");
        User other = persistUser("other@test.com", "other");
        Stream stream = persistStream(owner);

        em.persistAndFlush(new Like(owner, stream));
        em.clear();

        assertThat(likeRepository.findByStreamIdAndUserId(stream.getId(), other.getId()))
                .isEmpty();
    }

    private User persistUser(String email, String nickname) {
        return em.persistAndFlush(new User(email, "encoded", nickname));
    }

    private Stream persistStream(User user) {
        return em.persistAndFlush(
                new Stream("제목", "설명", null, "/uploads/a.mp4", user)
        );
    }
}
