package com.team2.fabackend.service.ledger;

import com.team2.fabackend.api.ledger.dto.LedgerRequest;
import com.team2.fabackend.domain.goals.Goal;
import com.team2.fabackend.domain.goals.GoalRepository;
import com.team2.fabackend.domain.ledger.Ledger;
import com.team2.fabackend.domain.ledger.LedgerRepository;
import com.team2.fabackend.domain.ledger.TransactionType;
import com.team2.fabackend.domain.user.User;
import com.team2.fabackend.service.user.UserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @InjectMocks
    private LedgerService ledgerService;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserReader userReader;

    @Test
    @DisplayName("가계부 내역 저장 - 지출 타입일 때 연동된 목표 업데이트 확인")
    void saveLedger_Expense_UpdatesGoals() {
        // given
        Long userId = 1L;
        LedgerRequest request = new LedgerRequest();
        ReflectionTestUtils.setField(request, "amount", 10000L);
        ReflectionTestUtils.setField(request, "category", "식비");
        ReflectionTestUtils.setField(request, "type", TransactionType.EXPENSE);
        ReflectionTestUtils.setField(request, "date", LocalDate.now());
        ReflectionTestUtils.setField(request, "time", LocalTime.now());

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Goal goal = Goal.builder()
                .category("식비")
                .currentAmount(5000L)
                .targetAmount(50000L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .build();

        given(userReader.findById(userId)).willReturn(user);
        given(goalRepository.findAllByUser_Id(userId)).willReturn(List.of(goal));

        // when
        ledgerService.saveLedger(userId, request);

        // then
        verify(ledgerRepository, times(1)).save(any(Ledger.class));
        assertThat(goal.getCurrentAmount()).isEqualTo(15000L); // 5000 + 10000
    }

    @Test
    @DisplayName("가계부 내역 저장 - 수입 타입일 때 목표 업데이트 되지 않음")
    void saveLedger_Income_NoGoalUpdate() {
        // given
        Long userId = 1L;
        LedgerRequest request = new LedgerRequest();
        ReflectionTestUtils.setField(request, "amount", 10000L);
        ReflectionTestUtils.setField(request, "category", "수입");
        ReflectionTestUtils.setField(request, "type", TransactionType.INCOME);

        User user = User.builder().build();
        given(userReader.findById(userId)).willReturn(user);

        // when
        ledgerService.saveLedger(userId, request);

        // then
        verify(ledgerRepository, times(1)).save(any(Ledger.class));
        verify(goalRepository, never()).findAllByUser_Id(any());
    }

    @Test
    @DisplayName("사용자별 가계부 내역 전체 조회")
    void findAllByUserId() {
        // given
        Long userId = 1L;
        Ledger ledger = Ledger.builder().amount(1000L).build();
        given(ledgerRepository.findAllByUser_Id(userId)).willReturn(List.of(ledger));

        // when
        List<Ledger> result = ledgerService.findAllByUserId(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("가계부 내역 수정")
    void update() {
        // given
        Long ledgerId = 1L;
        Ledger ledger = Ledger.builder()
                .amount(5000L)
                .category("식비")
                .build();
        
        LedgerRequest request = new LedgerRequest();
        ReflectionTestUtils.setField(request, "amount", 7000L);
        ReflectionTestUtils.setField(request, "category", "교통");
        ReflectionTestUtils.setField(request, "type", TransactionType.EXPENSE);

        given(ledgerRepository.findById(ledgerId)).willReturn(Optional.of(ledger));

        // when
        ledgerService.update(ledgerId, request);

        // then
        assertThat(ledger.getAmount()).isEqualTo(7000L);
        assertThat(ledger.getCategory()).isEqualTo("교통");
    }

    @Test
    @DisplayName("가계부 내역 삭제")
    void delete() {
        // given
        Long ledgerId = 1L;
        Ledger ledger = Ledger.builder().build();
        given(ledgerRepository.findById(ledgerId)).willReturn(Optional.of(ledger));

        // when
        ledgerService.delete(ledgerId);

        // then
        verify(ledgerRepository, times(1)).delete(ledger);
    }

    @Test
    @DisplayName("가계부 내역 삭제 - 존재하지 않는 경우 예외 발생")
    void delete_NotFound_ThrowsException() {
        // given
        Long ledgerId = 1L;
        given(ledgerRepository.findById(ledgerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> ledgerService.delete(ledgerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 내역이 없습니다.");
    }
}
