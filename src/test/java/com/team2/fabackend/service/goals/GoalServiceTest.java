package com.team2.fabackend.service.goals;

import com.team2.fabackend.api.goals.dto.GoalAnalysisResponse;
import com.team2.fabackend.api.goals.dto.GoalRequest;
import com.team2.fabackend.api.goals.dto.GoalResponse;
import com.team2.fabackend.domain.goals.Goal;
import com.team2.fabackend.domain.goals.GoalRepository;
import com.team2.fabackend.domain.ledger.LedgerRepository;
import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @InjectMocks
    private GoalService goalService;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("목표 생성 성공")
    void createGoal_Success() {
        // given
        Long userId = 1L;
        GoalRequest request = new GoalRequest();
        ReflectionTestUtils.setField(request, "title", "테스트 목표");
        ReflectionTestUtils.setField(request, "targetAmount", 100000L);
        ReflectionTestUtils.setField(request, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(request, "endDate", LocalDate.now().plusMonths(1));

        User user = User.builder().build();
        Goal goal = Goal.builder().id(1L).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(goalRepository.save(any(Goal.class))).willReturn(goal);

        // when
        Long goalId = goalService.createGoal(request, userId);

        // then
        assertThat(goalId).isEqualTo(1L);
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    @DisplayName("진행 중인 목표 조회")
    void findActiveGoals() {
        // given
        Long userId = 1L;
        Goal goal = Goal.builder()
                .id(1L)
                .title("진행중")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(5))
                .targetAmount(100000L)
                .currentAmount(20000L)
                .build();
        goal.calculateDailyAllowance();

        given(goalRepository.findAllByUser_Id(userId)).willReturn(List.of(goal));

        // when
        List<GoalResponse> result = goalService.findActiveGoals(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("진행중");
    }

    @Test
    @DisplayName("목표 분석 성공")
    void analyzeGoal_Success() {
        // given
        Long goalId = 1L;
        Goal goal = Goal.builder()
                .id(goalId)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .targetAmount(300000L)
                .currentAmount(50000L)
                .build();
        goal.calculateDailyAllowance(); // 300000 / 30 = 10000 per day

        given(goalRepository.findById(goalId)).willReturn(Optional.of(goal));

        // when
        GoalAnalysisResponse response = goalService.analyzeGoal(goalId);

        // then
        assertThat(response.getGoalId()).isEqualTo(goalId);
        assertThat(response.getType()).isIn("DELAYED", "SHORTENED");
    }

    @Test
    @DisplayName("목표 수정")
    void updateGoal() {
        // given
        Long goalId = 1L;
        Goal goal = Goal.builder()
                .title("이전 제목")
                .build();
        GoalRequest request = new GoalRequest();
        ReflectionTestUtils.setField(request, "title", "새 제목");
        ReflectionTestUtils.setField(request, "targetAmount", 200000L);
        ReflectionTestUtils.setField(request, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(request, "endDate", LocalDate.now().plusDays(30));

        given(goalRepository.findById(goalId)).willReturn(Optional.of(goal));

        // when
        goalService.updateGoal(goalId, request);

        // then
        assertThat(goal.getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("목표 삭제")
    void deleteGoal() {
        // given
        Long goalId = 1L;

        // when
        goalService.deleteGoal(goalId);

        // then
        verify(goalRepository, times(1)).deleteById(goalId);
    }
}
