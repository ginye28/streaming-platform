package com.sp.api.comment.repository;

import com.sp.api.comment.entity.Comment;
import com.sp.api.stream.entity.Stream;
import com.sp.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("댓글이 속하지 않은 영상 id 로는 조회되지 않는다")
    void findWithUserByIdAndStreamId_scopedToStream() {

        User user = em.persistAndFlush(new User("c@test.com", "encoded", "c"));
        Stream stream = em.persistAndFlush(new Stream("A", null, null, "/uploads/a.mp4", user));
        Stream otherStream = em.persistAndFlush(new Stream("B", null, null, "/uploads/b.mp4", user));

        Comment comment = em.persistAndFlush(new Comment("내용", stream, user));
        em.clear();

        assertThat(commentRepository.findWithUserByIdAndStreamId(comment.getId(), stream.getId()))
                .isPresent();

        assertThat(commentRepository.findWithUserByIdAndStreamId(comment.getId(), otherStream.getId()))
                .isEmpty();
    }
}
