package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StreamListIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("같은 사람이 여러 번 열어도 조회수는 한 번만 오른다")
    void viewCountRisesOncePerViewer() throws Exception {

        String owner = signupAndLogin("view-owner1@test.com", "조회주인1");
        long streamId = createStream(owner, "조회수영상1");
        String viewer = signupAndLogin("view-a1@test.com", "조회시청자A1");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/streams/" + streamId)
                            .header("Authorization", "Bearer " + viewer))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/streams/" + streamId)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    @Test
    @DisplayName("시청자가 다르면 조회수가 각각 오른다")
    void viewCountRisesForEachViewer() throws Exception {

        String owner = signupAndLogin("view-owner2@test.com", "조회주인2");
        long streamId = createStream(owner, "조회수영상2");

        String first = signupAndLogin("view-a2@test.com", "조회시청자A2");
        String second = signupAndLogin("view-b2@test.com", "조회시청자B2");

        mockMvc.perform(get("/api/streams/" + streamId).header("Authorization", "Bearer " + first))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(get("/api/streams/" + streamId).header("Authorization", "Bearer " + second))
                .andExpect(jsonPath("$.data.viewCount").value(2));

        // 먼저 본 사람이 다시 열어도 더 오르지 않는다
        mockMvc.perform(get("/api/streams/" + streamId).header("Authorization", "Bearer " + first))
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    @DisplayName("비로그인은 접속 IP 로 구분한다")
    void anonymousViewersAreSeparatedByIp() throws Exception {

        String owner = signupAndLogin("view-owner3@test.com", "조회주인3");
        long streamId = createStream(owner, "조회수영상3");

        mockMvc.perform(get("/api/streams/" + streamId).with(remoteAddr("10.0.0.1")))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(get("/api/streams/" + streamId).with(remoteAddr("10.0.0.1")))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(get("/api/streams/" + streamId).with(remoteAddr("10.0.0.2")))
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    @DisplayName("sortBy=POPULAR 는 조회수가 많은 영상을 앞에 둔다")
    void sortByPopular() throws Exception {

        String owner = signupAndLogin("sort-owner@test.com", "정렬주인");

        long quiet = createStream(owner, "조용한영상");
        long busy = createStream(owner, "인기영상");

        // 서로 다른 IP 로 열어서 조회수를 벌려 둔다
        view(busy, "10.1.0.1");
        view(busy, "10.1.0.2");
        view(quiet, "10.1.0.3");

        mockMvc.perform(get("/api/streams").param("sortBy", "POPULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("인기영상"))
                .andExpect(jsonPath("$.data.content[1].title").value("조용한영상"));
    }

    @Test
    @DisplayName("sortBy 를 주지 않으면 최신순이다")
    void defaultsToLatest() throws Exception {

        String owner = signupAndLogin("sort-owner2@test.com", "정렬주인2");

        createStream(owner, "먼저올린영상");
        createStream(owner, "나중에올린영상");

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("나중에올린영상"))
                .andExpect(jsonPath("$.data.content[1].title").value("먼저올린영상"));
    }

    @Test
    @DisplayName("없는 sortBy 값은 400 으로 막는다")
    void rejectsUnknownSortBy() throws Exception {

        mockMvc.perform(get("/api/streams").param("sortBy", "NO_SUCH_ORDER"))
                .andExpect(status().isBadRequest());
    }

    private void view(long streamId, String ip) throws Exception {
        mockMvc.perform(get("/api/streams/" + streamId).with(remoteAddr(ip)))
                .andExpect(status().isOk());
    }

    /** 비로그인 시청자를 IP 로 구분하므로 요청마다 접속 IP 를 바꿔 준다. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(String ip) {

        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
