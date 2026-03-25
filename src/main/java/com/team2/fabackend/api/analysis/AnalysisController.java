package com.team2.fabackend.api.analysis;

import com.team2.fabackend.api.analysis.dto.PersonalAnalysisResponse;
import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.service.Analysis.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(
        name = "Analysis",
        description = """
    ## 📊 소비 분석(Analysis) API
    
    사용자의 지출 내역을 기반으로 카테고리별, 요일별 소비 패턴을 분석합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **시각화**: 반환된 비중(percentage) 데이터를 사용하여 `MPAndroidChart` 등의 라이브러리로 원형/선 그래프를 구현하세요.
    - **성능 최적화**: 분석 데이터는 양이 많을 수 있으므로, 결과 수신 전까지 `Shimmer` 효과 등을 사용하여 사용자 경험을 개선하세요.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface AnalysisApi {
      @GET("/api/analysis/pattern/{userId}")
      suspend fun getPersonalAnalysis(@Path("userId") userId: Long): Response<PersonalAnalysisResponse>
    }
    ```
    """
)
public class AnalysisController {
    private final AnalysisService analysisService;

    /**
     * 특정 사용자의 개인별 소비 패턴 분석 결과를 조회합니다.
     *
     * @param userId 소비 패턴을 분석할 사용자의 ID.
     * @return 카테고리별 사용 내역, 주간 사용 내역, 소비 비율 통계가 포함된 PersonalAnalysisResponse.
     */
    @GetMapping("/pattern/{userId}")
    @Operation(
            summary = "개인 소비 패턴 분석 조회",
            description = "이번 달 지출 데이터를 분석하여 통계 수치를 제공합니다. 통계 탭 진입 시 API를 호출하여 그래프를 갱신하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 정보 없음")
    })
    public PersonalAnalysisResponse getPersonalAnalysis(@PathVariable Long userId) {
        return analysisService.getPersonalAnalysis(userId);
    }
}
