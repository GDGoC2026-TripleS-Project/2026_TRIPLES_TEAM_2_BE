package com.team2.fabackend.service.user;

import com.team2.fabackend.api.user.dto.UserDeleteRequest;
import com.team2.fabackend.api.user.dto.UserInfoRequest;
import com.team2.fabackend.api.user.dto.UserInfoResponse;
import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.domain.user.UserDeleteReason;
import com.team2.fabackend.global.exception.CustomException;
import com.team2.fabackend.service.auth.AuthVerificationService;
import com.team2.fabackend.service.auth.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserReader userReader;

    @Mock
    private UserWriter userWriter;

    @Mock
    private AuthVerificationService authVerificationService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("사용자 조회 성공")
    void getUser_Success() {
        // given
        Long userId = 1L;
        User user = User.builder().email("test@test.com").nickName("test").build();
        ReflectionTestUtils.setField(user, "id", userId);
        given(userReader.findById(userId)).willReturn(user);

        // when
        UserInfoResponse result = userService.getUser(userId);

        // then
        assertThat(result.getNickName()).isEqualTo("test");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("전체 사용자 목록 조회")
    void getAllUsers_Success() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        User user = User.builder().nickName("user1").build();
        given(userReader.findAllUsers(pageable)).willReturn(new PageImpl<>(List.of(user)));

        // when
        Page<UserInfoResponse> result = userService.getAllUsers(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getNickName()).isEqualTo("user1");
    }

    @Test
    @DisplayName("비밀번호 검증 성공 - 토큰 반환")
    void verifyCurrentPassword_Success() {
        // given
        Long userId = 1L;
        String rawPassword = "password123!";
        User user = User.builder().password("encoded-password").build();
        given(userReader.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(rawPassword, "encoded-password")).willReturn(true);

        // when
        String token = userService.verifyCurrentPassword(userId, rawPassword);

        // then
        assertThat(token).isNotNull();
        verify(authVerificationService).saveVerificationToken(eq(userId), any(String.class));
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 예외 발생")
    void verifyCurrentPassword_Fail() {
        // given
        Long userId = 1L;
        String rawPassword = "wrong-password";
        User user = User.builder().password("encoded-password").build();
        given(userReader.findById(userId)).willReturn(user);
        given(passwordEncoder.matches(rawPassword, "encoded-password")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.verifyCurrentPassword(userId, rawPassword))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_Success() {
        // given
        Long userId = 1L;
        String token = "valid-token";
        String newPassword = "newPassword123!";
        User user = User.builder().build();
        given(userReader.findById(userId)).willReturn(user);
        given(passwordEncoder.encode(newPassword)).willReturn("new-encoded-password");

        // when
        userService.updatePassword(userId, token, newPassword);

        // then
        verify(authVerificationService).validateVerificationToken(userId, token);
        verify(userWriter).updatePassword(user, "new-encoded-password");
        verify(authVerificationService).deleteVerification(userId);
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_Success() {
        // given
        Long userId = 1L;
        String token = "valid-token";
        UserInfoRequest request = new UserInfoRequest("새닉네임", LocalDate.of(2000, 1, 1));
        User user = User.builder().build();
        given(userReader.findById(userId)).willReturn(user);

        // when
        userService.updateProfile(userId, token, request);

        // then
        verify(authVerificationService).validateVerificationToken(userId, token);
        verify(userWriter).updateProfile(user, "새닉네임", LocalDate.of(2000, 1, 1));
        verify(authVerificationService).deleteVerification(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteUser_Success() {
        // given
        Long userId = 1L;
        String token = "valid-token";
        UserDeleteRequest request = new UserDeleteRequest("사유", "상세사유");
        User user = User.builder().birth(LocalDate.of(1990, 1, 1)).build();
        given(userReader.findById(userId)).willReturn(user);

        // when
        userService.deleteUser(userId, token, request);

        // then
        verify(authVerificationService).validateVerificationToken(userId, token);
        verify(userWriter).createReason(any(UserDeleteReason.class));
        verify(userWriter).delete(user);
        verify(authVerificationService).deleteVerification(userId);
        verify(refreshTokenService).deleteRefreshToken(userId);
    }
}
