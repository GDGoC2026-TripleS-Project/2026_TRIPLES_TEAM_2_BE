package com.team2.fabackend.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청 데이터 모델 (Android ViewModel에서 수집하여 서버로 전송 시 사용)")
public class LoginRequest {
    @Schema(description = "사용자 이메일 (아이디)", example = "android@toktory.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String email;

    @Schema(description = "비밀번호 (EditText 입력값)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String password;
}
