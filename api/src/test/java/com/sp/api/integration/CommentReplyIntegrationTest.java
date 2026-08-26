package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentReplyIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("답글은 원 댓글에 묶여서 오고, 목록에는 원 댓글만 실린다")
    void repliesComeNestedUnderTheirParent() throws Exception {

        String owner = signupAndLogin("reply-owner1@test.com", "답글주인1");
        long streamId = createStream(owner, "답글영상1");

        long parentId = comment(owner, streamId, "원 댓글", null);
        comment(owner, streamId, "첫 답글", parentId);
        comment(owner, streamId, "둘째 답글", parentId);

        mockMvc.perform(get("/api/streams/" + streamId + "/comments"))
                .andExpect(status().isOk())
                // 답글은 원 댓글 자리를 차지하지 않는다
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].content").value("원 댓글"))
                .andExpect(jsonPath("$.data.content[0].parentId").doesNotExist())
                // 답글끼리는 쓴 순서대로
                .andExpect(jsonPath("$.data.content[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].replies[0].content").value("첫 답글"))
                .andExpect(jsonPath("$.data.content[0].replies[1].content").value("둘째 답글"))
                .andExpect(jsonPath("$.data.content[0].replies[0].parentId").value(parentId));
    }

    @Test
    @DisplayName("답글에는 다시 답글을 달 수 없다")
    void repliesCannotBeNestedTwice() throws Exception {

        String owner = signupAndLogin("reply-owner2@test.com", "답글주인2");
        long streamId = createStream(owner, "답글영상2");

        long parentId = comment(owner, streamId, "원 댓글", null);
        long replyId = comment(owner, streamId, "답글", parentId);

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"답글의 답글","parentId":%d}""".formatted(replyId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 영상의 댓글에는 답글을 달 수 없다")
    void cannotReplyAcrossStreams() throws Exception {

        String owner = signupAndLogin("reply-owner3@test.com", "답글주인3");
        long streamId = createStream(owner, "답글영상3");
        long otherStreamId = createStream(owner, "다른영상3");

        long parentId = comment(owner, streamId, "원 댓글", null);

        mockMvc.perform(post("/api/streams/" + otherStreamId + "/comments")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"남의 영상에 답글","parentId":%d}""".formatted(parentId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("원 댓글을 지우면 달려 있던 답글도 함께 사라진다")
    void deletingAParentRemovesItsReplies() throws Exception {

        String owner = signupAndLogin("reply-owner4@test.com", "답글주인4");
        long streamId = createStream(owner, "답글영상4");

        long parentId = comment(owner, streamId, "지울 원 댓글", null);
        comment(owner, streamId, "같이 지워질 답글", parentId);

        mockMvc.perform(delete("/api/streams/" + streamId + "/comments/" + parentId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/streams/" + streamId + "/comments"))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // 답글까지 사라졌으므로 영상의 댓글 수도 0 이다
        mockMvc.perform(get("/api/streams/" + streamId))
                .andExpect(jsonPath("$.data.commentCount").value(0));
    }

    @Test
    @DisplayName("영상의 댓글 수에는 답글도 포함된다")
    void commentCountIncludesReplies() throws Exception {

        String owner = signupAndLogin("reply-owner5@test.com", "답글주인5");
        long streamId = createStream(owner, "답글영상5");

        long parentId = comment(owner, streamId, "원 댓글", null);
        comment(owner, streamId, "답글", parentId);

        mockMvc.perform(get("/api/streams/" + streamId))
                .andExpect(jsonPath("$.data.commentCount").value(2));
    }

    @Test
    @DisplayName("답글이 달린 영상도 삭제된다")
    void streamWithRepliesCanBeDeleted() throws Exception {

        String owner = signupAndLogin("reply-owner6@test.com", "답글주인6");
        long streamId = createStream(owner, "답글영상6");

        long parentId = comment(owner, streamId, "원 댓글", null);
        comment(owner, streamId, "답글", parentId);

        // 답글이 원 댓글을 FK 로 참조하므로 지우는 순서가 틀리면 여기서 터진다
        mockMvc.perform(delete("/api/streams/" + streamId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/streams/" + streamId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("답글이 달리면 원 댓글 작성자에게 알림이 간다")
    void replyNotifiesTheParentAuthor() throws Exception {

        String owner = signupAndLogin("noti-owner@test.com", "알림주인");
        String asker = signupAndLogin("noti-asker@test.com", "알림질문자");
        long streamId = createStream(owner, "알림영상1");

        long parentId = comment(asker, streamId, "이거 어떻게 하나요?", null);
        comment(owner, streamId, "이렇게 하시면 됩니다", parentId);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + asker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("COMMENT_REPLY"))
                .andExpect(jsonPath("$.data.content[0].message")
                        .value("알림주인 님이 회원님의 댓글에 답글을 남겼습니다."))
                // 눌렀을 때 영상으로 갈 수 있어야 한다
                .andExpect(jsonPath("$.data.content[0].targetId").value(streamId))
                .andExpect(jsonPath("$.data.content[0].read").value(false));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + asker))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    @DisplayName("자기 댓글에 자기가 단 답글은 알림을 남기지 않는다")
    void replyingToYourselfNotifiesNobody() throws Exception {

        String owner = signupAndLogin("noti-self@test.com", "알림혼잣말");
        long streamId = createStream(owner, "알림영상2");

        long parentId = comment(owner, streamId, "원 댓글", null);
        comment(owner, streamId, "아 참 그리고", parentId);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("답글이 아닌 새 댓글은 알림을 남기지 않는다")
    void plainCommentsNotifyNobody() throws Exception {

        String owner = signupAndLogin("noti-owner3@test.com", "알림주인3");
        String guest = signupAndLogin("noti-guest3@test.com", "알림손님3");
        long streamId = createStream(owner, "알림영상3");

        comment(guest, streamId, "영상 잘 봤습니다", null);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    /** @return 만들어진 댓글 id. parentId 가 null 이면 원 댓글이다. */
    private long comment(String token, long streamId, String content, Long parentId) throws Exception {

        String body = parentId == null
                ? """
                {"content":"%s"}""".formatted(content)
                : """
                {"content":"%s","parentId":%d}""".formatted(content, parentId);

        MvcResult result = mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("data").path("id").asLong();
    }
}
