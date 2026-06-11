package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.enums.SettlementStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.ExpenseSplitEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.SettlementEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ExpenseSplitJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PaymentProofJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.SettlementJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;

class SettlementServiceTest {
    private SettlementJpaRepository settlementJpaRepository;
    private ExpenseSplitJpaRepository expenseSplitJpaRepository;
    private PaymentProofJpaRepository paymentProofJpaRepository;
    private BankAccountJpaRepository bankAccountJpaRepository;
    private TripService tripService;
    private SettlementService settlementService;

    private UserEntity userA;
    private UserEntity userB;
    private UserEntity userC;
    private TripEntity trip;

    @BeforeEach
    void setUp() {
        settlementJpaRepository = mock(SettlementJpaRepository.class);
        expenseSplitJpaRepository = mock(ExpenseSplitJpaRepository.class);
        paymentProofJpaRepository = mock(PaymentProofJpaRepository.class);
        bankAccountJpaRepository = mock(BankAccountJpaRepository.class);
        tripService = mock(TripService.class);
        settlementService = new SettlementService(
                settlementJpaRepository,
                expenseSplitJpaRepository,
                paymentProofJpaRepository,
                bankAccountJpaRepository,
                tripService,
                mock(FileStorage.class),
                mock(TripJpaRepository.class));

        userA = user(1L, "A");
        userB = user(2L, "B");
        userC = user(3L, "C");
        trip = TripEntity.builder().id(12L).name("Trip").leader(userA).build();

        when(tripService.findUser(1L)).thenReturn(userA);
        when(tripService.findUser(2L)).thenReturn(userB);
        when(tripService.findUser(3L)).thenReturn(userC);
        when(bankAccountJpaRepository.findFirstByUserIdAndIsDefaultTrue(any())).thenReturn(Optional.empty());

        AtomicLong ids = new AtomicLong(30L);
        Answer<SettlementEntity> saveAnswer = invocation -> {
            SettlementEntity settlement = invocation.getArgument(0);
            if (settlement.getId() == null) {
                settlement.setId(ids.incrementAndGet());
            }
            return settlement;
        };
        when(settlementJpaRepository.save(any(SettlementEntity.class))).thenAnswer(saveAnswer);
    }

    @Test
    void generateSettlements_createsPaymentsFromMembersToLeaderForTheirSplitShares() {
        when(settlementJpaRepository.existsByTripId(12L)).thenReturn(false);
        when(expenseSplitJpaRepository.findByExpenseTripId(12L)).thenReturn(List.of(
                split(expense(101L, userA, "900000"), userA, "300000"),
                split(expense(101L, userA, "900000"), userB, "300000"),
                split(expense(101L, userA, "900000"), userC, "300000"),
                split(expense(102L, userA, "300000"), userA, "100000"),
                split(expense(102L, userA, "300000"), userB, "100000"),
                split(expense(102L, userA, "300000"), userC, "100000")));

        var result = settlementService.generateSettlements(trip);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(settlement -> {
            assertThat(settlement.fromUserId()).isEqualTo(2L);
            assertThat(settlement.toUserId()).isEqualTo(1L);
            assertThat(settlement.amount()).isEqualByComparingTo(new BigDecimal("400000"));
            assertThat(settlement.transferContent()).startsWith("TRIP-12-SETTLEMENT-");
        });
        assertThat(result).anySatisfy(settlement -> {
            assertThat(settlement.fromUserId()).isEqualTo(3L);
            assertThat(settlement.toUserId()).isEqualTo(1L);
            assertThat(settlement.amount()).isEqualByComparingTo(new BigDecimal("400000"));
        });
    }

    @Test
    void confirm_requiresReceiverAndUploadedProof() {
        SettlementEntity settlement = SettlementEntity.builder()
                .id(34L)
                .trip(trip)
                .fromUser(userB)
                .toUser(userA)
                .amount(new BigDecimal("100000"))
                .status(SettlementStatus.PROOF_UPLOADED)
                .build();
        when(settlementJpaRepository.findById(34L)).thenReturn(Optional.of(settlement));
        when(tripService.requireActiveMemberTrip(12L, 2L)).thenReturn(trip);

        assertThatThrownBy(() -> settlementService.confirm(34L, 2L))
                .isInstanceOf(ForbiddenTripActionException.class);
    }

    private UserEntity user(Long id, String name) {
        return UserEntity.builder().id(id).username(name).name(name).build();
    }

    private TripExpenseEntity expense(Long id, UserEntity paidBy, String amount) {
        return TripExpenseEntity.builder()
                .id(id)
                .trip(trip)
                .paidBy(paidBy)
                .amount(new BigDecimal(amount))
                .build();
    }

    private ExpenseSplitEntity split(TripExpenseEntity expense, UserEntity user, String amount) {
        return ExpenseSplitEntity.builder()
                .expense(expense)
                .user(user)
                .amount(new BigDecimal(amount))
                .build();
    }
}
