package com.team2.fabackend.api.budget;

import com.team2.fabackend.api.budget.dto.BudgetRequest;
import com.team2.fabackend.api.budget.dto.BudgetResponse;
import com.team2.fabackend.api.budget.dto.BudgetUpdateRequest;
import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.domain.budget.BudgetGoal;
import com.team2.fabackend.service.budget.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@Tag(
        name = "Budget",
        description = """
    ## 💰 예산 설정(Budget) API
    
    사용자의 소비 설문 데이터를 기반으로 권장 예산을 생성하고 관리합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **설문 결과 처리**: 설문 결과를 전송하면 서버가 4대 카테고리 예산을 자동 계산합니다. 이를 UI에 프리셋으로 보여주고 사용자가 수정할 수 있게 하세요.
    - **상태 관리**: 설정된 예산 정보는 앱 전체에서 공유되어야 하므로 `StateFlow` 등을 이용해 전역적으로 관리하는 것이 좋습니다.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface BudgetApi {
      @POST("/api/budget/{userId}")
      suspend fun saveBudget(@Path("userId") userId: Long, @Body request: BudgetRequest): Response<Long>
      
      @GET("/api/budget/{userId}")
      suspend fun getBudget(@Path("userId") userId: Long): Response<BudgetResponse>
    }
    ```
    """
)
public class BudgetController {
    private final BudgetService budgetService;

    /**
     * 사용자가 입력한 설문 항목을 바탕으로 카테고리별 예산을 계산하여 저장하거나 기존 예산을 업데이트합니다.
     * 
     * @param request 설문 옵션 정보가 담긴 DTO
     * @param userId 유저 식별자
     * @return 생성 또는 수정된 예산 목표의 ID
     */
    @PostMapping("/{userId}")
    @Operation(
            summary = "예산 설정 (설문 기반)",
            description = "설문 조사 결과를 서버로 전송하여 권장 예산을 생성합니다. 성공 시 생성된 Budget ID가 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "404", description = "유저 정보를 찾을 수 없음")
    })
    public Long saveBudget(@RequestBody BudgetRequest request, @PathVariable @Parameter(description = "유저 ID", example = "1") Long userId) {
        return budgetService.saveBudget(request, userId);
    }

    /**
     * 특정 사용자의 현재 예산 목표 설정 정보를 조회합니다.
     * 
     * @param userId 유저 식별자
     * @return 카테고리별 예산 금액 정보가 담긴 응답 DTO
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "현재 예산 조회",
            description = "앱 초기화 단계나 설정 화면에서 사용자의 현재 예산 설정 정보를 불러올 때 사용합니다."
    )
    public BudgetResponse getBudget(@PathVariable @Parameter(description = "유저 ID", example = "1") Long userId) {
        return budgetService.getBudget(userId);
    }

    /**
     * 사용자가 직접 입력한 금액으로 각 카테고리별 예산을 수정합니다.
     * 
     * @param userId 유저 식별자
     * @param request 수정할 카테고리별 금액 정보가 담긴 DTO
     * @return 수정된 예산 목표의 ID
     */
    @PatchMapping("/{userId}/amounts")
    @Operation(
            summary = "예산 금액 직접 수정",
            description = "사용자가 예산 편집 화면에서 직접 숫자를 입력하여 수정할 때 사용합니다."
    )
    public Long updateAmounts(@PathVariable @Parameter(description = "유저 ID", example = "1") Long userId, @RequestBody BudgetUpdateRequest request) {
        return budgetService.updateBudgetAmounts(userId, request);
    }
}
