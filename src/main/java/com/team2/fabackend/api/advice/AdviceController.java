package com.team2.fabackend.api.advice;

import com.team2.fabackend.api.advice.dto.AdviceMessageResponse;
import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.service.advice.AdviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("advice")
@RequiredArgsConstructor
@Tag(
        name = "Advice",
        description = """
    ## 🐿️ 맞춤 조언(Advice) API
    
    사용자의 예산 및 지출 내역을 분석하여 캐릭터(다람쥐)가 제공하는 오늘의 조언을 관리합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **UI 연동**: 조언 내용에 따라 캐릭터의 감정(ChipmunkStatus)이 달라지므로, 이를 기반으로 앱 내 캐릭터 리소스를 변경하세요.
    - **호출 시점**: 앱 메인 화면 진입 시 `ViewModel`에서 호출하여 초기 데이터를 로드하는 것을 권장합니다.
    - **중복 처리**: 하루에 한 번만 생성되므로, 여러 번 호출해도 동일한 날짜에는 같은 데이터가 반환됩니다.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface AdviceApi {
      @POST("/advice/generate")
      suspend fun generateAdvice(): Response<AdviceMessageResponse>
    }
    ```
    """
)
public class AdviceController {

    private final AdviceService adviceService;

    /**
     * 인증된 사용자를 위한 오늘의 맞춤형 소비 조언을 생성하거나 조회합니다.
     *
     * @param userId 인증된 사용자의 ID
     * @return 조언 메시지와 상태를 포함하는 ResponseEntity
     */
    @PostMapping("/generate")
    @Operation(
            summary = "오늘의 맞춤 조언 생성/조회",
            description = "사용자의 소비 패턴을 분석한 AI 조언을 가져옵니다. 메인 화면의 대시보드나 캐릭터 클릭 시 활용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조언 생성/조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken 만료)"),
            @ApiResponse(responseCode = "404", description = "데이터 부족 (지출 내역이 없는 경우)")
    })
    public ResponseEntity<AdviceMessageResponse> generateAdvice(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(adviceService.generateAdvice(userId));
    }
}
