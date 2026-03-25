package com.team2.fabackend.api.user.dto;

import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.global.enums.SocialType;
import com.team2.fabackend.global.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "로그인 유저 본인 또는 타인의 프로필 정보 응답 모델")
public class UserInfoResponse {
    @Schema(description = "사용자 고유 식별 ID (서버 DB PK)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "사용자 계정 이메일 (마스킹 없이 반환)", example = "android@toktory.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "연동된 소셜 계정 타입 (KAKAO, NAVER, GOOGLE 또는 일반 가입일 경우 NONE)", example = "NONE", requiredMode = Schema.RequiredMode.REQUIRED)
    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Schema(description = "앱 내 표시되는 사용자 닉네임", example = "코틀린마스터", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickName;

    @Schema(description = "사용자 생년월일 (yyyy-MM-dd)", example = "2002-04-01", format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birth;

    @Schema(description = "사용자 권한 등급 (일반: USER, 관리자: ADMIN)", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserType userType;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .socialType(user.getSocialType())
                .nickName(user.getNickName())
                .birth(user.getBirth())
                .userType(user.getUserType())
                .build();
    }
}
