package com.team2.fabackend.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "신규 회원가입 요청 데이터 (이메일 인증 완료 후 최종 가입 단계에서 전송)")
public class SignupRequest {
    @NotBlank
    @Email
    @Schema(description = "사용자 이메일 계정 (로그인 ID로 사용될 고유 이메일)", example = "android@toktory.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    @Schema(description = "사용자 비밀번호 (8~20자, 영문/숫자/특수문자 포함 권장)", example = "password123!", minLength = 8, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    private String password;

    @Schema(description = "사용자 앱 내 닉네임", example = "코틀린마스터", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickName;

    @NotNull
    @Schema(description = "사용자 생년월일 (yyyy-MM-dd)", example = "2002-04-01", format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birth;

    @NotNull
    @Schema(description = "FCM 토큰 또는 기기 고유 ID (푸시 알림 및 기기 식별용)", example = "fcm_token_xyz_123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;
}
