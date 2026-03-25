package com.team2.fabackend.api.goals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "저축 목표 상세 정보 (목표 목록 및 상세 화면 렌더링용)")
public class GoalResponse {
    @Schema(description = "목표 고유 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "목표 제목 (사용자가 설정한 이름)", example = "아이맥 사기", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "선택한 카테고리", example = "전자제품", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String category;

    @Schema(description = "최종 달성해야 할 금액", example = "3000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetAmount;

    @Schema(description = "현재까지 가계부 '저축'으로 모은 누적 금액", example = "1500000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long currentSpend;

    @Schema(description = "현재 목표 진행 상태 (IN_PROGRESS, ACHIEVED, FAILED 등)", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "현재 달성률 (0~100 사이의 정수)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    public int progressRate;

    @Schema(description = "카테고리별 저축 분포 상세 통계", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<CategoryStatResponse> categoryStats;

    @Schema(description = "AI가 예측한 최종 성공 확률 (%)", example = "85.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private double successRate;

    @Schema(description = "목표 마감일까지 남은 일수", example = "180", requiredMode = Schema.RequiredMode.REQUIRED)
    private long changedDays;

    @Schema(description = "지연 여부 (현재 속도로 마감일까지 달성이 불가능한지 여부)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isDelayed;
}
