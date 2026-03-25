package com.team2.fabackend.api.user;

import com.team2.fabackend.api.user.dto.PasswordRequest;
import com.team2.fabackend.api.user.dto.UserDeleteRequest;
import com.team2.fabackend.api.user.dto.UserInfoRequest;
import com.team2.fabackend.api.user.dto.UserInfoResponse;
import com.team2.fabackend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@Tag(
        name = "User",
        description = """
    ## 👤 사용자 관리(User) API
    
    프로필 조회, 수정 및 보안 인증을 통한 비밀번호 변경, 탈퇴 기능을 제공합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **보안 인증 (Confirm Token)**: 닉네임 수정, 비밀번호 변경, 탈퇴 등 민감한 작업은 `비밀번호 검증(/me/password/verify)`이 선행되어야 합니다.
    - **헤더 관리**: 검증 성공 시 반환된 토큰을 `X-Password-Confirm-Token` 헤더에 담아 후속 요청을 보내세요. 이 토큰은 1회용이거나 수명이 매우 짧습니다.
    - **이미지 처리**: 프로필 이미지 URL이 제공될 경우 `Coil` 또는 `Glide` 라이브러리를 사용해 캐싱 및 로딩을 처리하세요.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface UserApi {
      @GET("/users/me")
      suspend fun getMyProfile(): Response<UserInfoResponse>
      
      @PATCH("/users/me")
      suspend fun updateProfile(
        @Header("X-Password-Confirm-Token") token: String,
        @Body request: UserInfoRequest
      ): Response<Unit>
    }
    ```
    """
)
public class UserController {
    private final UserService userService;

    /**
     * 현재 인증된 사용자의 정보를 조회합니다.
     *
     * @param userId 인증된 사용자의 ID
     * @return 사용자의 정보를 포함한 ResponseEntity
     */
    @GetMapping("/me")
    @Operation(
            summary = "내 프로필 정보 조회",
            description = "로그인한 유저의 정보를 가져옵니다. 홈 화면이나 마이페이지 초기화 시 호출하세요."
    )
    public ResponseEntity<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    /**
     * 다른 사용자의 공개 프로필 정보를 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 정보를 포함한 ResponseEntity
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "타 사용자 프로필 조회",
            description = "상대방의 공개된 프로필을 조회할 때 사용합니다."
    )
    public ResponseEntity<UserInfoResponse> getUser(@PathVariable @Parameter(description = "대상 유저 ID", example = "2") Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    /**
     * 전체 사용자 목록을 페이징하여 조회합니다.
     *
     * @param pageable 페이징 및 정렬 정보
     * @return 사용자 정보 페이지를 포함한 ResponseEntity
     */
    @GetMapping
    @Operation(
            summary = "전체 사용자 목록 조회 (관리자용)",
            description = "전체 리스트를 페이징하여 가져옵니다. 안드로이드의 `Paging3` 라이브러리와 연동하기 좋습니다."
    )
    public ResponseEntity<Page<UserInfoResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    /**
     * 사용자의 현재 비밀번호를 검증하고 헤더에 짧은 수명의 확인 토큰을 발급합니다.
     *
     * @param userId  인증된 사용자의 ID
     * @param request 비밀번호 검증 요청 상세 정보
     * @return "X-Password-Confirm-Token" 헤더에 확인 토큰을 포함한 ResponseEntity
     */
    @PostMapping("/me/password/verify")
    @Operation(
            summary = "비밀번호 검증 (보안 인증)",
            description = "정보 수정 전 본인 확인을 위해 현재 비밀번호를 입력받습니다. 성공 시 헤더에서 **X-Password-Confirm-Token**을 추출하여 저장하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치")
    })
    public ResponseEntity<Void> verify(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordRequest.Verify request
    ) {
        String token = userService.verifyCurrentPassword(userId, request.currentPassword());
        return ResponseEntity.ok()
                .header("X-Password-Confirm-Token", token)
                .build();
    }

    /**
     * 사용자의 프로필 정보를 수정합니다. 유효한 비밀번호 확인 토큰이 필요합니다.
     *
     * @param userId               인증된 사용자의 ID
     * @param passwordConfirmToken 비밀번호 검증으로 발급받은 확인 토큰
     * @param request              수정된 사용자 정보
     * @return 성공 시 200 OK 상태의 ResponseEntity
     */
    @PatchMapping("/me")
    @Operation(
            summary = "내 프로필 정보 수정",
            description = "닉네임, 생년월일 등을 변경합니다. 앞서 발급받은 **Confirm Token**이 헤더에 반드시 포함되어야 합니다."
    )
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestHeader("X-Password-Confirm-Token") @Parameter(description = "비밀번호 검증 후 받은 토큰") String passwordConfirmToken,
            @Valid @RequestBody UserInfoRequest request
    ) {
        userService.updateProfile(userId, passwordConfirmToken, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자의 비밀번호를 변경합니다. 유효한 비밀번호 확인 토큰이 필요합니다.
     *
     * @param userId               인증된 사용자의 ID
     * @param passwordConfirmToken 비밀번호 검증으로 발급받은 확인 토큰
     * @param request              새로운 비밀번호를 포함한 요청 객체
     * @return 성공 시 204 No Content 상태의 ResponseEntity
     */
    @PatchMapping("/me/password")
    @Operation(
            summary = "비밀번호 변경",
            description = "로그인된 상태에서 비밀번호를 새것으로 변경합니다. **Confirm Token**이 필요합니다."
    )
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Long userId,
            @RequestHeader("X-Password-Confirm-Token") @Parameter(description = "비밀번호 검증 후 받은 토큰") String passwordConfirmToken,
            @Valid @RequestBody PasswordRequest.Update request
    ) {
        userService.updatePassword(userId, passwordConfirmToken, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * 인증된 사용자의 계정을 삭제합니다. 유효한 비밀번호 확인 토큰과 사유가 필요합니다.
     *
     * @param userId               인증된 사용자의 ID
     * @param passwordConfirmToken 비밀번호 검증으로 발급받은 확인 토큰
     * @param request              탈퇴 사유를 포함한 사용자 탈퇴 요청 객체
     * @return 성공 시 200 OK 상태의 ResponseEntity
     */
    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "계정을 완전히 삭제합니다. 탈퇴 완료 후에는 저장된 토큰을 즉시 폐기하고 초기 화면으로 이동시키세요."
    )
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal Long userId,
            @RequestHeader("X-Password-Confirm-Token") @Parameter(description = "비밀번호 검증 후 받은 토큰") String passwordConfirmToken,
            @Valid @RequestBody UserDeleteRequest request
    ) {
        userService.deleteUser(userId, passwordConfirmToken, request);
        return ResponseEntity.ok().build();
    }
}
