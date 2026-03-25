package com.team2.fabackend.api.auth;

import com.team2.fabackend.api.auth.dto.LoginRequest;
import com.team2.fabackend.api.auth.dto.LoginResponse;
import com.team2.fabackend.api.auth.dto.RefreshRequest;
import com.team2.fabackend.api.auth.dto.SignupRequest;
import com.team2.fabackend.api.auth.dto.TokenPair;
import com.team2.fabackend.api.email.dto.EmailSendRequest;
import com.team2.fabackend.api.email.dto.EmailVerifyRequest;
import com.team2.fabackend.service.phoneVerification.EmailVerificationService;
import com.team2.fabackend.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth",
        description = """
    ## 🔐 인증 및 계정 관리(Auth) API
    
    회원가입, 로그인, 토큰 갱신 및 비밀번호 찾기 기능을 제공합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **인증 토큰**: Bearer JWT를 사용하며, 발급된 Access Token은 모든 요청의 `Authorization` 헤더(Bearer Prefix 포함)에 담아야 합니다.
    - **토큰 저장**: 보안을 위해 Refresh Token은 `EncryptedSharedPreferences` 또는 `Jetpack DataStore`에 저장하는 것을 권장합니다.
    - **이메일 인증**: 모든 가입 및 비밀번호 찾기 과정에서 OTP 인증이 필수적입니다.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface AuthApi {
      @POST("/auth/login")
      suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
      
      @POST("/auth/refresh")
      suspend fun refresh(@Body request: RefreshRequest): Response<LoginResponse>
    }
    ```
    """
)
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 사용자의 회원가입 요청을 처리합니다.
     * 이메일 인증이 선행되어야 하며, 입력 데이터의 유효성을 검증합니다.
     * 
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임 등)
     * @return 성공 시 200 OK
     */
    @PostMapping("/signup")
    @Operation(
            summary = "신규 회원가입",
            description = "이메일, 비밀번호, 닉네임 등으로 가입합니다. 이메일 인증(/auth/email/verify)이 먼저 완료되어야 가입 처리가 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공 (로그인 화면으로 이동 권장)"),
            @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패 (이메일 형식, 비밀번호 규칙 등)"),
            @ApiResponse(responseCode = "403", description = "이메일 인증 미완료"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    })
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 입력된 이메일의 사용 가능 여부를 확인합니다.
     * 중복되지 않은 경우 성공 응답을 반환합니다.
     * 
     * @param email 중복 체크할 이메일
     * @return 성공 시 200 OK (캐시 제어 헤더 포함)
     */
    @GetMapping("/check-email")
    @Operation(
            summary = "이메일 중복 확인",
            description = "사용자가 입력한 이메일의 사용 가능 여부를 실시간으로 체크할 때 사용합니다. EditText 포커스 아웃 시 호출하면 좋습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public ResponseEntity<Void> checkEmail(@RequestParam @Parameter(description = "확인할 이메일", example = "user@example.com") String email) {
        authService.checkEmailDuplication(email);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS))
                .build();
    }

    /**
     * 사용자의 이메일과 비밀번호를 확인하여 로그인을 처리합니다.
     * 성공 시 Access Token은 헤더에, Refresh Token은 바디에 담아 반환합니다.
     * 
     * @param request 로그인 정보 (이메일, 비밀번호)
     * @return Refresh Token을 포함한 응답 바디 및 Access Token을 포함한 헤더
     */
    @PostMapping("/login")
    @Operation(
            summary = "로그인 (토큰 발급)",
            description = "로그인 성공 시 Access Token은 **Authorization 헤더(Bearer)**로, Refresh Token은 **Body**로 전달됩니다. 두 토큰 모두 앱 내부 저장소에 저장하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 오류")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenPair tokens = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken())
                .body(new LoginResponse(tokens.getRefreshToken()));
    }

    /**
     * 만료된 Access Token을 Refresh Token을 사용하여 재발급합니다.
     * 
     * @param request Refresh Token이 포함된 요청
     * @return 새로운 Access Token(헤더) 및 Refresh Token(바디)
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "토큰 갱신 (Refresh)",
            description = "Access Token이 만료되어 HTTP 401이 반환될 경우 호출합니다. **Retrofit Authenticator** 등을 활용해 자동으로 처리하는 것이 좋습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "갱신 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 만료 (다시 로그인 필요)")
    })
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        TokenPair tokenPair = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.getAccessToken())
                .body(new LoginResponse(tokenPair.getRefreshToken()));
    }

    /**
     * 사용자의 로그아웃을 처리하여 서버 측 세션 정보를 삭제합니다.
     * 
     * @param userId 로그아웃할 사용자의 식별자
     * @return 성공 시 200 OK
     */
    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "서버의 Refresh Token을 무효화합니다. 성공 시 앱 내 저장된 모든 토큰 정보를 삭제하고 로그인 화면으로 전환하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않음")
    })
    public ResponseEntity<Void> logout(@RequestParam @Parameter(description = "유저 ID", example = "1") Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 찾기를 위한 인증번호를 발송합니다.
     * 가입된 이메일인 경우에만 발송됩니다.
     * 
     * @param request 대상 이메일 정보
     * @return 성공 시 200 OK
     */
    @PostMapping("/find/send-code")
    @Operation(
            summary = "비밀번호 찾기 - 인증코드 발송",
            description = "비밀번호 재설정을 위해 가입된 이메일로 6자리 인증 코드를 보냅니다."
    )
    public ResponseEntity<Void> sendFindCode(@RequestBody @Valid EmailSendRequest request) {
        emailVerificationService.sendCodeForFinding(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * 이메일 인증 후 임시 비밀번호를 발송합니다.
     * 
     * @param request 이메일 및 인증번호 정보
     * @return 성공 시 200 OK
     */
    @PostMapping("/find/password")
    @Operation(
            summary = "비밀번호 찾기 - 임시 비밀번호 발급",
            description = "인증 코드 검증 후, 해당 이메일로 임시 비밀번호를 전송합니다."
    )
    public ResponseEntity<Void> sendTemporaryPassword(@RequestBody @Valid EmailVerifyRequest request) {
        authService.sendTemporaryPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원가입을 위한 인증번호를 발송합니다.
     * 이미 가입된 이메일인 경우 에러를 반환합니다.
     * 
     * @param request 대상 이메일 정보
     * @return 성공 시 200 OK
     */
    @PostMapping("/signup/send-code")
    @Operation(
            summary = "회원가입 - 인증코드 발송",
            description = "신규 가입을 위해 입력한 이메일로 6자리 인증 코드를 보냅니다. 중복 이메일은 409를 반환합니다."
    )
    public ResponseEntity<Void> sendSignUpCode(@RequestBody @Valid EmailSendRequest request) {
        emailVerificationService.sendCodeForSignUp(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자가 입력한 인증번호를 검증합니다.
     * 검증 성공 시 해당 이메일은 일정 시간 동안 인증된 상태로 유지됩니다.
     * 
     * @param request 이메일과 인증번호 정보
     * @return 성공 시 200 OK
     */
    @PostMapping("/email/verify")
    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "사용자가 입력한 6자리 코드가 일치하는지 확인합니다. 성공 시 가입 처리가 가능해집니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "코드 불일치 또는 만료")
    })
    public ResponseEntity<Void> verifyEmailCode(@RequestBody @Valid EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }
}
