package com.team2.fabackend.api.goals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.fabackend.api.goals.dto.GoalAnalysisResponse;
import com.team2.fabackend.api.goals.dto.GoalRequest;
import com.team2.fabackend.api.goals.dto.GoalResponse;
import com.team2.fabackend.global.security.JwtProvider;
import com.team2.fabackend.service.goals.GoalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalService goalService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("저축 목표 생성 성공")
    void createGoal_Success() throws Exception {
        // given
        GoalRequest request = new GoalRequest();
        ReflectionTestUtils.setField(request, "title", "아이맥 사기");
        ReflectionTestUtils.setField(request, "category", "전자제품");
        ReflectionTestUtils.setField(request, "targetAmount", 3000000L);
        ReflectionTestUtils.setField(request, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(request, "endDate", LocalDate.now().plusMonths(6));

        given(goalService.createGoal(any(GoalRequest.class), eq(1L))).willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/goals")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @DisplayName("전체 목표 목록 조회 성공")
    void getGoalList_Success() throws Exception {
        // given
        GoalResponse goal = GoalResponse.builder()
                .id(1L)
                .title("아이맥 사기")
                .targetAmount(3000000L)
                .build();
        given(goalService.findAllGoals()).willReturn(List.of(goal));

        // when & then
        mockMvc.perform(get("/api/goals/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("아이맥 사기"));
    }

    @Test
    @DisplayName("진행 중인 목표 조회 성공")
    void getActiveGoals_Success() throws Exception {
        // given
        Long userId = 1L;
        GoalResponse goal = GoalResponse.builder()
                .id(1L)
                .title("진행 중인 목표")
                .build();
        given(goalService.findActiveGoals(userId)).willReturn(List.of(goal));

        // when & then
        mockMvc.perform(get("/api/goals/active/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("진행 중인 목표"));
    }

    @Test
    @DisplayName("목표 정보 수정 성공")
    void updateGoal_Success() throws Exception {
        // given
        Long goalId = 1L;
        GoalRequest request = new GoalRequest();
        ReflectionTestUtils.setField(request, "title", "수정된 제목");

        // when & then
        mockMvc.perform(patch("/api/goals/{id}", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(goalService).updateGoal(eq(goalId), any(GoalRequest.class));
    }

    @Test
    @DisplayName("목표 삭제 성공")
    void deleteGoal_Success() throws Exception {
        // given
        Long goalId = 1L;

        // when & then
        mockMvc.perform(delete("/api/goals/{id}", goalId))
                .andExpect(status().isOk());

        verify(goalService).deleteGoal(goalId);
    }

    @Test
    @DisplayName("목표 AI 달성 분석 성공")
    void analyzeGoal_Success() throws Exception {
        // given
        Long goalId = 1L;
        GoalAnalysisResponse response = GoalAnalysisResponse.builder()
                .goalId(goalId)
                .analysisMessage("잘하고 있어요!")
                .successRate(85.5)
                .build();
        given(goalService.analyzeGoal(goalId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/goals/{id}/analysis", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(goalId))
                .andExpect(jsonPath("$.analysisMessage").value("잘하고 있어요!"))
                .andExpect(jsonPath("$.successRate").value(85.5));
    }
}
