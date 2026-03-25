package com.team2.fabackend.api.term;

import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.api.term.dto.AgreedTermRequest;
import com.team2.fabackend.api.term.dto.TermInfoResponse;
import com.team2.fabackend.api.term.dto.TermSaveRequest;
import com.team2.fabackend.api.term.dto.UserTermStatusResponse;
import com.team2.fabackend.service.userTerm.UserTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
@Tag(
        name = "Term",
        description = """
    ## 📜 약관 관리(Term) API
    
    서비스 이용을 위한 필수 및 선택 약관을 조회하고 동의 여부를 관리합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **동의 여부 저장**: 사용자의 약관 동의 상태는 서버에서 관리되지만, 앱 최초 실행 여부 판단 등을 위해 로컬 DB(`Room`)나 `DataStore`에 간단히 플래그를 저장해두면 초기 UI 분기에 유용합니다.
    - **약관 상세 내역**: `TermInfoResponse`에서 제공하는 URL 또는 텍스트 내용을 `WebView`나 `TextView`를 통해 사용자에게 보여주세요.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface TermApi {
      @GET("/terms/active")
      suspend fun getActiveTerms(): Response<List<TermInfoResponse>>
      
      @POST("/terms/agree")
      suspend fun agreeTerms(@Body request: AgreedTermRequest): Response<Unit>
    }
    ```
    """
)
public class TermController {
    private final UserTermService userTermService;

    /**
     * 현재 활성화된 약관 목록을 조회합니다.
     *
     * @return 활성화된 약관 목록을 포함한 ResponseEntity
     */
    @GetMapping("/active")
    @Operation(
            summary = "유효 약관 목록 조회",
            description = "현재 서비스에서 시행 중인 최신 약관 목록을 조회합니다. 회원가입 화면의 체크박스 리스트 생성 시 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "약관 목록 조회 성공")
    })
    public ResponseEntity<List<TermInfoResponse>> getActiveTerms() {
        return ResponseEntity.ok(userTermService.getActiveTerms());
    }

    /**
     * 현재 인증된 사용자의 약관 동의 현황을 조회합니다.
     *
     * @param userId 인증된 사용자의 ID
     * @return 약관 목록과 동의 현황을 포함한 ResponseEntity
     */
    @GetMapping("/me")
    @Operation(
            summary = "내 약관 동의 현황 조회",
            description = "로그인한 사용자가 동의한 약관 목록을 가져옵니다. 설정 화면의 약관 정보 확인란에서 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (AccessToken 만료)")
    })
    public ResponseEntity<List<UserTermStatusResponse>> getUserTermStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(userTermService.getUserTermStatus(userId));
    }

    /**
     * 인증된 사용자의 약관 동의를 처리합니다.
     *
     * @param userId  인증된 사용자의 ID
     * @param request 동의한 약관 ID 목록을 포함하는 요청 객체
     * @return 성공 시 200 OK 상태의 ResponseEntity
     */
    @PostMapping("/agree")
    @Operation(
            summary = "약관 동의 처리",
            description = "회원가입 절차 중 약관 동의를 완료했을 때 호출합니다. 선택 약관 동의 여부도 포함하세요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동의 처리 완료"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 약관 ID 포함")
    })
    public ResponseEntity<Void> agreeTerms(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AgreedTermRequest request
    ) {
        userTermService.agreeTerms(userId, request.getAgreedTermIds());
        return ResponseEntity.ok().build();
    }

    /**
     * 새로운 약관 레코드를 생성합니다. 이 작업은 관리자 권한이 필요합니다.
     *
     * @param request 저장할 약관 상세 정보
     * @return 생성된 약관 정보를 포함하는 ResponseEntity
     */
    @PostMapping
    @Operation(
            summary = "[ADMIN] 새 약관 등록",
            description = "시스템 관리자가 새로운 약관을 등록합니다. (관리자 전용)"
    )
    public ResponseEntity<TermInfoResponse> createTerm(
            @Valid @RequestBody TermSaveRequest request
    ) {
        return ResponseEntity.ok(userTermService.createTerm(request));
    }

    /**
     * 기존 약관 레코드를 수정합니다. 이 작업은 관리자 권한이 필요합니다.
     *
     * @param termId  수정할 약관의 ID
     * @param request 새로운 약관 상세 정보
     * @return 수정된 약관 정보를 포함하는 ResponseEntity
     */
    @PatchMapping
    @Operation(
            summary = "[ADMIN] 약관 정보 수정",
            description = "등록된 약관의 제목이나 내용을 수정합니다. (관리자 전용)"
    )
    public ResponseEntity<TermInfoResponse> updateTerm(
            @RequestParam @Parameter(description = "약관 ID", example = "1") Long termId,
            @Valid @RequestBody TermSaveRequest request
    ) {
        return ResponseEntity.ok(userTermService.updateTerm(termId, request));
    }
}
