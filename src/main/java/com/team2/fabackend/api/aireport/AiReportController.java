package com.team2.fabackend.api.aireport;

import com.team2.fabackend.api.aireport.dto.AiReportRequest;
import com.team2.fabackend.api.aireport.dto.AiReportResponse;
import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.service.mail.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Tag(
        name = "AI Report",
        description = """
    ## 📧 AI 리포트(Report) API
    
    사용자의 지출 내역을 AI가 분석하여 이메일로 상세 리포트를 전송합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **비동기 처리**: 서버에서 메일 발송은 백그라운드로 처리되므로, 클라이언트는 성공 응답 수신 후 "리포트 발송 중" 안내 메시지를 표시하면 됩니다.
    - **입력 유효성**: 이메일 주소 형식 검증은 앱 프런트엔드에서 먼저 수행하는 것을 권장합니다.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface AiReportApi {
      @POST("/report/send")
      suspend fun sendAiReport(@Body request: AiReportRequest): Response<AiReportResponse>
    }
    ```
    """
)
public class AiReportController {

    private final MailService mailService;

    /**
     * AI 기반 소비 리포트를 생성하고 지정된 수신자의 이메일로 전송합니다.
     *
     * @param userId  인증된 사용자의 ID.
     * @param request 수신자의 이메일 주소를 포함한 요청 객체.
     * @return 리포트 생성 및 전송 프로세스의 결과 메시지를 포함한 ResponseEntity.
     */
    @Operation(
            summary = "AI 리포트 이메일 발송",
            description = "사용자의 소비 분석 결과가 담긴 PDF 리포트를 입력한 이메일로 전송합니다. 로딩 애니메이션과 함께 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 요청 성공 (메일함 확인 유도)"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken 만료)")
    })
    @PostMapping("/send")
    public ResponseEntity<AiReportResponse> sendAiReport(
            @AuthenticationPrincipal Long userId,
            @RequestBody AiReportRequest request
    ) {
        return ResponseEntity.ok(mailService.sendAiReport(userId, request.getReceiverEmail()));
    }
}
