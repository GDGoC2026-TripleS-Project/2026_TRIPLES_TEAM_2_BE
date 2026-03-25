package com.team2.fabackend.api.ledger;

import com.team2.fabackend.api.error.dto.ErrorResponse;
import com.team2.fabackend.api.ledger.dto.LedgerRequest;
import com.team2.fabackend.api.ledger.dto.LedgerResponse;
import com.team2.fabackend.domain.ledger.Ledger;
import com.team2.fabackend.domain.user.User; 
import com.team2.fabackend.service.ledger.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; 
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Tag(
        name = "Ledger",
        description = """
    ## 📑 가계부(Ledger) API
    
    사용자의 지출 및 수입 내역을 기록하고 관리합니다.
    
    ---
    
    ### 🔑 안드로이드 구현 가이드
    - **입력 폼**: '저축' 카테고리 선택 시 현재 진행 중인 목표에 금액이 합산되므로, 사용자에게 이를 알리는 UI 피드백을 주면 좋습니다.
    - **리스트 갱신**: 내역 추가/수정/삭제 후에는 가계부 메인 리스트와 대시보드(예산 현황)를 모두 새로고침해야 합니다.
    - **날짜 형식**: 날짜 데이터 전송 시 서버 규격(ISO_LOCAL_DATE 등)을 확인하여 포맷팅하세요.
    
    ### 🧩 Kotlin / Retrofit 예시
    ```kotlin
    interface LedgerApi {
      @POST("/api/ledger/add")
      suspend fun addLedger(@Body request: LedgerRequest): Response<Unit>
      
      @GET("/api/ledger/list")
      suspend fun getLedgers(): Response<List<Ledger>>
    }
    ```
    """
)
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * 인증된 사용자의 새로운 가계부 내역을 저장하고 관련 목표에 자동으로 반영합니다.
     *
     * @param userId  인증된 사용자의 ID.
     * @param request 저장할 가계부 내역 상세 정보.
     * @return 저장 성공 시 OK 상태를 포함하는 ResponseEntity.
     */
    @PostMapping("/add")
    @Operation(
            summary = "가계부 내역 추가",
            description = "지출 또는 수입 내역을 저장합니다. '저축' 카테고리를 선택하면 활성 목표의 달성 금액에 합산됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 만료)")
    })
    public ResponseEntity<Void> addLedger(
            @AuthenticationPrincipal Long userId,
            @RequestBody LedgerRequest request) {
        ledgerService.saveLedger(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 인증된 사용자의 모든 가계부 내역을 조회합니다.
     *
     * @param userId 인증된 사용자의 ID.
     * @return 사용자의 가계부 내역 목록을 포함하는 ResponseEntity.
     */
    @GetMapping("/list")
    @Operation(
            summary = "가계부 내역 전체 조회",
            description = "사용자의 전체 소비/수입 내역을 가져옵니다. `RecyclerView` 등을 사용하여 리스트를 구성하세요."
    )
    public ResponseEntity<List<Ledger>> getAllLedgers(
            @AuthenticationPrincipal Long userId
    ) {
        List<Ledger> responses = ledgerService.findAllByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * ID를 통해 기존 가계부 내역을 수정하고 연결된 목표에 변경 사항을 반영합니다.
     *
     * @param id      수정할 가계부 내역의 ID.
     * @param userId  인증된 사용자의 ID.
     * @param request 수정된 가계부 내역 상세 정보.
     * @return 수정 성공 시 OK 상태를 포함하는 ResponseEntity.
     */
    @PatchMapping("/{id}")
    @Operation(
            summary = "가계부 내역 수정",
            description = "기존 내역의 금액, 카테고리 등을 수정합니다. 금액 변경 시 연동된 목표 수치도 자동 재계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 완료"),
            @ApiResponse(responseCode = "404", description = "내역 정보 없음")
    })
    public ResponseEntity<Void> updateLedger(
            @PathVariable("id") @Parameter(description = "가계부 내역 ID", example = "101") Long id,
            @AuthenticationPrincipal Long userId,
            @RequestBody LedgerRequest request
    ) {
        ledgerService.update(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * ID를 통해 특정 가계부 내역을 삭제하고 관련 목표 금액을 그에 맞춰 조정합니다.
     *
     * @param id     삭제할 가계부 내역의 ID.
     * @param userId 인증된 사용자의 ID.
     * @return 삭제 성공 시 OK 상태를 포함하는 ResponseEntity.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "가계부 내역 삭제",
            description = "내역을 삭제합니다. 삭제된 금액만큼 예산 및 목표 달성 수치가 복구됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 완료"),
            @ApiResponse(responseCode = "404", description = "내역 정보 없음")
    })
    public ResponseEntity<Void> deleteLedger(
            @PathVariable("id") @Parameter(description = "가계부 내역 ID", example = "101") Long id,
            @AuthenticationPrincipal Long userId
    ) {
        ledgerService.delete(id);
        return ResponseEntity.ok().build();
    }
}
