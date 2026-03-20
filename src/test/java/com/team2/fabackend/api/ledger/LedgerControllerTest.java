package com.team2.fabackend.api.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.fabackend.api.ledger.dto.LedgerRequest;
import com.team2.fabackend.domain.ledger.Ledger;
import com.team2.fabackend.domain.ledger.TransactionType;
import com.team2.fabackend.global.security.JwtProvider;
import com.team2.fabackend.service.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
@AutoConfigureMockMvc(addFilters = false)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("가계부 내역 추가 성공")
    void addLedger_Success() throws Exception {
        // given
        LedgerRequest request = new LedgerRequest();
        ReflectionTestUtils.setField(request, "amount", 10000L);
        ReflectionTestUtils.setField(request, "category", "식비");
        ReflectionTestUtils.setField(request, "type", TransactionType.EXPENSE);

        // when & then
        mockMvc.perform(post("/api/ledger/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(ledgerService).saveLedger(any(), any(LedgerRequest.class));
    }

    @Test
    @DisplayName("가계부 내역 전체 조회")
    void getAllLedgers_Success() throws Exception {
        // given
        Ledger ledger = Ledger.builder().id(1L).amount(5000L).build();
        given(ledgerService.findAllByUserId(any())).willReturn(List.of(ledger));

        // when & then
        mockMvc.perform(get("/api/ledger/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(5000));
    }

    @Test
    @DisplayName("가계부 내역 수정 성공")
    void updateLedger_Success() throws Exception {
        // given
        Long ledgerId = 1L;
        LedgerRequest request = new LedgerRequest();
        ReflectionTestUtils.setField(request, "amount", 8000L);

        // when & then
        mockMvc.perform(patch("/api/ledger/{id}", ledgerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(ledgerService).update(eq(ledgerId), any(LedgerRequest.class));
    }

    @Test
    @DisplayName("가계부 내역 삭제 성공")
    void deleteLedger_Success() throws Exception {
        // given
        Long ledgerId = 1L;

        // when & then
        mockMvc.perform(delete("/api/ledger/{id}", ledgerId))
                .andExpect(status().isOk());

        verify(ledgerService).delete(ledgerId);
    }
}
