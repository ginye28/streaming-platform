package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 콜백 비밀 값(RTMP_CALLBACK_TOKEN)을 켠 서버. 재생 주소에 드러나는 방송 이름만으로는
 * 남의 방송을 끝낼 수 없어야 한다.
 */
@TestPropertySource(properties = "app.rtmp.callback-token=secret-callback")
class RtmpCallbackTokenIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("비밀 값 없이 방송 종료를 호출하면 거절하고 방송은 그대로 둔다")
    void publishDoneWithoutTokenIsRejected() throws Exception {

        String token = signupAndLogin("tok-done@test.com", "종료막기");
        String publicName = startBroadcastWithToken(token);

        mockMvc.perform(post("/api/internal/rtmp/publish-done").param("name", publicName))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/internal/rtmp/publish-done")
                        .param("name", publicName).param("token", "틀린-값"))
                .andExpect(status().isForbidden());

        // 방송은 아직 켜져 있어서 같은 이름으로 다시 들어오는 송출이 허용된다
        mockMvc.perform(post("/api/internal/rtmp/publish")
                        .param("name", publicName).param("token", "secret-callback"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀 값 없이 송출 시작을 호출하면 거절한다")
    void publishWithoutTokenIsRejected() throws Exception {

        String token = signupAndLogin("tok-pub@test.com", "시작막기");
        String streamKey = streamKeyOf(token);

        mockMvc.perform(post("/api/internal/rtmp/publish").param("name", streamKey))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("맞는 비밀 값이면 송출을 시작하고 끝낼 수 있다")
    void validTokenIsAccepted() throws Exception {

        String token = signupAndLogin("tok-ok@test.com", "정상");
        String streamKey = streamKeyOf(token);

        mockMvc.perform(post("/api/internal/rtmp/publish")
                        .param("name", streamKey).param("token", "secret-callback"))
                .andExpect(status().isFound());
    }

    private String startBroadcastWithToken(String userToken) throws Exception {

        String streamKey = streamKeyOf(userToken);

        String location = mockMvc.perform(post("/api/internal/rtmp/publish")
                        .param("name", streamKey).param("token", "secret-callback"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getHeader("Location");

        return location.substring(location.lastIndexOf('/') + 1);
    }
}
