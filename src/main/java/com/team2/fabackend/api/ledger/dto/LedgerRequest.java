package com.team2.fabackend.api.ledger.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team2.fabackend.domain.ledger.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Schema(description = "가계부 지출/수입 내역 등록 및 수정 데이터 모델")
public class LedgerRequest {
    @Schema(description = "금액 (정수형)", example = "15000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long amount;

    @Schema(description = "카테고리 명칭 (예: 식비, 교통, 여가, 저축 등)", example = "식비", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Schema(description = "사용자가 직접 입력한 메모", example = "점심 돈까스", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String memo;

    @Schema(description = "거래 유형 (INCOME: 수입, EXPENDITURE: 지출)", example = "EXPENDITURE", requiredMode = Schema.RequiredMode.REQUIRED)
    private TransactionType type;

    @Schema(description = "발생 날짜 (yyyy-MM-dd)", example = "2026-03-19", format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date = LocalDate.now();

    @Schema(description = "발생 시간 (HH:mm)", example = "12:30", format = "time", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "HH:mm")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime time = LocalTime.now();
}
