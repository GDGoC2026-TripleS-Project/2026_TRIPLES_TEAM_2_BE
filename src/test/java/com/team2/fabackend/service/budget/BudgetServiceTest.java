package com.team2.fabackend.service.budget;

import com.team2.fabackend.api.budget.dto.BudgetRequest;
import com.team2.fabackend.api.budget.dto.BudgetResponse;
import com.team2.fabackend.api.budget.dto.BudgetUpdateRequest;
import com.team2.fabackend.domain.budget.BudgetGoal;
import com.team2.fabackend.domain.budget.BudgetRepository;
import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @InjectMocks
    private BudgetService budgetService;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("설문 기반 예산 저장 - 새로운 예산 생성")
    void saveBudget_New() {
        // given
        Long userId = 1L;
        BudgetRequest request = new BudgetRequest();
        ReflectionTestUtils.setField(request, "foodDailyOption", 1); // 75000
        ReflectionTestUtils.setField(request, "deliveryFreqOption", 1); // 0
        ReflectionTestUtils.setField(request, "dessertCostOption", 1); // 10000
        // Total food should be 85000

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(budgetRepository.findByUserId(userId)).willReturn(Optional.empty());
        
        BudgetGoal savedGoal = BudgetGoal.builder().user(user).foodAmount(85000L).build();
        ReflectionTestUtils.setField(savedGoal, "id", 100L);
        given(budgetRepository.save(any(BudgetGoal.class))).willReturn(savedGoal);

        // when
        Long resultId = budgetService.saveBudget(request, userId);

        // then
        assertThat(resultId).isEqualTo(100L);
        verify(budgetRepository).save(any(BudgetGoal.class));
    }

    @Test
    @DisplayName("예산 조회 성공")
    void getBudget_Success() {
        // given
        Long userId = 1L;
        BudgetGoal goal = BudgetGoal.builder()
                .foodAmount(300000L)
                .transportAmount(100000L)
                .leisureAmount(200000L)
                .fixedAmount(400000L)
                .build();
        ReflectionTestUtils.setField(goal, "id", 100L);
        given(budgetRepository.findByUserId(userId)).willReturn(Optional.of(goal));

        // when
        BudgetResponse response = budgetService.getBudget(userId);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getFoodAmount()).isEqualTo(300000L);
        assertThat(response.getTotalAmount()).isEqualTo(1000000L);
    }

    @Test
    @DisplayName("예산 직접 수정 성공")
    void updateBudgetAmounts_Success() {
        // given
        Long userId = 1L;
        BudgetGoal goal = BudgetGoal.builder()
                .foodAmount(300000L)
                .build();
        ReflectionTestUtils.setField(goal, "id", 100L);

        BudgetUpdateRequest request = new BudgetUpdateRequest();
        ReflectionTestUtils.setField(request, "foodAmount", 500000L);
        ReflectionTestUtils.setField(request, "transportAmount", 100000L);
        ReflectionTestUtils.setField(request, "leisureAmount", 200000L);
        ReflectionTestUtils.setField(request, "fixedAmount", 400000L);

        given(budgetRepository.findByUserId(userId)).willReturn(Optional.of(goal));

        // when
        Long resultId = budgetService.updateBudgetAmounts(userId, request);

        // then
        assertThat(resultId).isEqualTo(100L);
        assertThat(goal.getFoodAmount()).isEqualTo(500000L);
    }
}
