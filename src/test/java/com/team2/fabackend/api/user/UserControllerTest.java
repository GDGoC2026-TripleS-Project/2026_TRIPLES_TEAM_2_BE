package com.team2.fabackend.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.fabackend.api.user.dto.PasswordRequest;
import com.team2.fabackend.api.user.dto.UserDeleteRequest;
import com.team2.fabackend.api.user.dto.UserInfoRequest;
import com.team2.fabackend.api.user.dto.UserInfoResponse;
import com.team2.fabackend.global.enums.SocialType;
import com.team2.fabackend.global.enums.UserType;
import com.team2.fabackend.global.security.JwtProvider;
import com.team2.fabackend.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("내 프로필 정보 조회 성공")
    void getCurrentUser_Success() throws Exception {
        // given
        Long userId = 1L;
        UserInfoResponse response = UserInfoResponse.builder()
                .id(userId)
                .email("test@example.com")
                .nickName("무말랭이")
                .socialType(SocialType.LOCAL)
                .userType(UserType.USER)
                .build();
        given(userService.getUser(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickName").value("무말랭이"));
    }

    @Test
    @DisplayName("타 사용자 프로필 조회 성공")
    void getUser_Success() throws Exception {
        // given
        Long targetId = 2L;
        UserInfoResponse response = UserInfoResponse.builder()
                .id(targetId)
                .nickName("다른사람")
                .build();
        given(userService.getUser(targetId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/{userId}", targetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickName").value("다른사람"));
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getAllUsers_Success() throws Exception {
        // given
        UserInfoResponse user1 = UserInfoResponse.builder().id(1L).nickName("user1").build();
        given(userService.getAllUsers(any())).willReturn(new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 1));

        // when & then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nickName").value("user1"));
    }

    @Test
    @DisplayName("비밀번호 검증 성공 및 토큰 발급")
    void verifyPassword_Success() throws Exception {
        // given
        PasswordRequest.Verify request = new PasswordRequest.Verify("password123!");
        given(userService.verifyCurrentPassword(any(), eq("password123!"))).willReturn("confirm-token-uuid");

        // when & then
        mockMvc.perform(post("/users/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Password-Confirm-Token", "confirm-token-uuid"));
    }

    @Test
    @DisplayName("내 프로필 정보 수정 성공")
    void updateProfile_Success() throws Exception {
        // given
        UserInfoRequest request = new UserInfoRequest("새닉네임", LocalDate.of(2000, 1, 1));
        String token = "valid-token";

        // when & then
        mockMvc.perform(patch("/users/me")
                        .header("X-Password-Confirm-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateProfile(any(), eq(token), any(UserInfoRequest.class));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_Success() throws Exception {
        // given
        PasswordRequest.Update request = new PasswordRequest.Update("newpassword123!");
        String token = "valid-token";

        // when & then
        mockMvc.perform(patch("/users/me/password")
                        .header("X-Password-Confirm-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updatePassword(any(), eq(token), eq("newpassword123!"));
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteUser_Success() throws Exception {
        // given
        UserDeleteRequest request = new UserDeleteRequest("이유", "상세이유");
        String token = "valid-token";

        // when & then
        mockMvc.perform(delete("/users/me")
                        .header("X-Password-Confirm-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).deleteUser(any(), eq(token), any(UserDeleteRequest.class));
    }
}
