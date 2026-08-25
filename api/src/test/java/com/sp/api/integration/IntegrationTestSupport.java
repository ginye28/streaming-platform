package com.sp.api.integration;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통 준비물. 같은 설정을 쓰므로 스프링 컨텍스트가 재사용된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtProvider jwtProvider;

    /**
     * ADMIN 권한 계정을 만들고 바로 쓸 수 있는 토큰을 돌려준다.
     * 관리자 가입 API 가 따로 없으므로 테스트에서만 권한을 직접 부여한다.
     */
    protected String adminToken(String email, String nickname) throws Exception {

        User admin = new User(email, passwordEncoder.encode("password123"), nickname);
        ReflectionTestUtils.setField(admin, "role", User.Role.ADMIN);
        userRepository.saveAndFlush(admin);

        return jwtProvider.createToken(email);
    }

    protected void signup(String email, String nickname) throws Exception {

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","nickname":"%s"}"""
                                .formatted(email, nickname)))
                .andExpect(status().isCreated());
    }

    protected String login(String email) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        String token = json(result).path("data").path("accessToken").asString();

        assertThat(token).isNotBlank();

        return token;
    }

    /** 가입 후 바로 로그인해 토큰을 돌려준다. */
    protected String signupAndLogin(String email, String nickname) throws Exception {
        signup(email, nickname);
        return login(email);
    }

    protected long createStream(String token, String title) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"설명","videoUrl":"/uploads/a.mp4"}"""
                                .formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("data").path("id").asLong();
    }

    protected long myUserId(String token) throws Exception {

        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/users/me")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return json(result).path("data").path("id").asLong();
    }

    protected String streamKeyOf(String token) throws Exception {

        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/users/stream-key")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return json(result).path("data").path("streamKey").asString();
    }

    /** nginx-rtmp 의 on_publish 콜백을 흉내 내 방송을 시작시킨다. @return 공개 이름 */
    protected String startBroadcast(String token) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/internal/rtmp/publish")
                        .param("name", streamKeyOf(token)))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        assertThat(location).isNotNull();

        return location.substring(location.lastIndexOf('/') + 1);
    }

    protected void endBroadcast(String publicName) throws Exception {

        mockMvc.perform(post("/api/internal/rtmp/publish-done").param("name", publicName))
                .andExpect(status().isOk());
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
