package com.team2.fabackend.service.auth;

import com.team2.fabackend.api.auth.dto.LoginRequest;
import com.team2.fabackend.api.auth.dto.SignupRequest;
import com.team2.fabackend.api.auth.dto.TokenPair;
import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.global.enums.AccountStatus;
import com.team2.fabackend.global.enums.ErrorCode;
import com.team2.fabackend.global.enums.SocialType;
import com.team2.fabackend.global.enums.UserType;
import com.team2.fabackend.global.exception.CustomException;
import com.team2.fabackend.global.security.JwtProvider;
import com.team2.fabackend.service.mail.MailService;
import com.team2.fabackend.service.phoneVerification.EmailVerificationService;
import com.team2.fabackend.service.user.UserReader;
import com.team2.fabackend.service.user.UserWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserReader userReader;

    @Mock
    private UserWriter userWriter;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123!");
        ReflectionTestUtils.setField(request, "nickName", "tester");
        ReflectionTestUtils.setField(request, "birth", LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(request, "deviceId", "device123");

        given(userReader.existsByEmail(anyString())).willReturn(false);
        given(userReader.existsByDeviceId(anyString())).willReturn(false);
        given(userReader.existsByNickName(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(emailVerificationService).checkVerified("test@test.com");
        verify(userWriter).create(any(User.class));
        verify(emailVerificationService).clearVerificationLog("test@test.com");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_DuplicateEmail() {
        // given
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");

        given(userReader.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_USER_ID);
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123!");

        User mockUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .userType(UserType.USER)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        given(userReader.findByEmailAndSocialType("test@test.com", SocialType.LOCAL)).willReturn(mockUser);
        given(passwordEncoder.matches("password123!", "encodedPassword")).willReturn(true);
        given(jwtProvider.createAccessToken(1L, UserType.USER)).willReturn("accessToken");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refreshToken");

        // when
        TokenPair tokenPair = authService.login(request);

        // then
        assertThat(tokenPair.getAccessToken()).isEqualTo("accessToken");
        assertThat(tokenPair.getRefreshToken()).isEqualTo("refreshToken");
        verify(refreshTokenService).saveRefreshToken(anyLong(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_InvalidPassword() {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "wrongPassword");

        User mockUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .build();

        given(userReader.findByEmailAndSocialType("test@test.com", SocialType.LOCAL)).willReturn(mockUser);
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PASSWORD);
        
        assertThat(mockUser.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금 상태")
    void login_Fail_LockedAccount() {
        // given
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");

        User mockUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .build();
        mockUser.updateAccountStatus(AccountStatus.LOCKED);

        given(userReader.findByEmailAndSocialType("test@test.com", SocialType.LOCAL)).willReturn(mockUser);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_LOCKED);
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    void refreshAccessToken_Success() {
        // given
        String oldRefreshToken = "oldRefreshToken";
        User mockUser = User.builder()
                .email("test@test.com")
                .userType(UserType.USER)
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        given(jwtProvider.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(oldRefreshToken)).willReturn(1L);
        given(userReader.findById(1L)).willReturn(mockUser);
        given(jwtProvider.createAccessToken(1L, UserType.USER)).willReturn("newAccessToken");
        given(jwtProvider.createRefreshToken(1L)).willReturn("newRefreshToken");

        // when
        TokenPair tokenPair = authService.refreshAccessToken(oldRefreshToken);

        // then
        assertThat(tokenPair.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(tokenPair.getRefreshToken()).isEqualTo("newRefreshToken");
        verify(refreshTokenService).validateRefreshToken(1L, oldRefreshToken);
        verify(refreshTokenService).saveRefreshToken(anyLong(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logout_Success() {
        // given
        Long userId = 1L;

        // when
        authService.logout(userId);

        // then
        verify(refreshTokenService).deleteRefreshToken(userId);
    }
}
