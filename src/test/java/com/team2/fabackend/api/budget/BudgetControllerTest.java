package com.team2.fabackend.api.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.fabackend.api.budget.dto.BudgetRequest;
import com.team2.fabackend.api.budget.dto.BudgetResponse;
import com.team2.fabackend.api.budget.dto.BudgetUpdateRequest;
import com.team2.fabackend.domain.budget.BudgetGoal;
import com.team2.fabackend.global.security.JwtProvider;
import com.team2.fabackend.service.budget.BudgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("설문 기반 예산 설정 성공")
    void saveBudget_Success() throws Exception {
        // given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest();
        ReflectionTestUtils.setField(request, "foodDailyOption", 1);
        ReflectionTestUtils.setField(request, "deliveryFreqOption", 1);

        given(budgetService.saveBudget(any(BudgetRequest.class), eq(userId))).willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/budget/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    @DisplayName("현재 예산 조회 성공")
    void getBudget_Success() throws Exception {
        // given
        Long userId = 1L;
        BudgetGoal budgetGoal = BudgetGoal.builder()
                .foodAmount(300000L)
                .transportAmount(100000L)
                .leisureAmount(200000L)
                .fixedAmount(400000L)
                .build();
        ReflectionTestUtils.setField(budgetGoal, "id", 100L);

        BudgetResponse response = new BudgetResponse(budgetGoal);
        given(budgetService.getBudget(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/budget/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.foodAmount").value(300000))
                .andExpect(jsonPath("$.totalAmount").value(1000000));
    }

    @Test
    @DisplayName("예산 금액 직접 수정 성공")
    void updateAmounts_Success() throws Exception {
        // given
        Long userId = 1L;
        BudgetUpdateRequest request = new BudgetUpdateRequest();
        ReflectionTestUtils.setField(request, "foodAmount", 500000L);

        given(budgetService.updateBudgetAmounts(eq(userId), any(BudgetUpdateRequest.class))).willReturn(100L);

        // when & then
        mockMvc.perform(patch("/api/budget/{userId}/amounts", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }
}
