package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripExpenseRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripExpenseRequest;
import edu.uet.travel_hub.application.dto.response.TripExpenseContributionResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseSummaryResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseTransactionResponse;
import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripExpenseJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;

@Service
public class TripExpenseService {
    private final TripService tripService;
    private final TripExpenseJpaRepository tripExpenseJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final TripActivityLogService tripActivityLogService;

    public TripExpenseService(
            TripService tripService,
            TripExpenseJpaRepository tripExpenseJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            TripActivityLogService tripActivityLogService) {
        this.tripService = tripService;
        this.tripExpenseJpaRepository = tripExpenseJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
    }

    @Transactional(readOnly = true)
    public TripExpenseResponse listExpenses(Long tripId, Long currentUserId) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripEntity trip = this.tripService.findTrip(tripId);
        List<TripExpenseEntity> expenses = this.tripExpenseJpaRepository.findByTripIdOrderByExpenseDateDescIdDesc(tripId);

        BigDecimal totalSpent = this.tripExpenseJpaRepository
                .sumAmountByTripId(tripId)
                .orElse(BigDecimal.ZERO);

        Map<Long, BigDecimal> contributions = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getPaidBy().getId(),
                        Collectors.mapping(TripExpenseEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        long approvedMemberCount = this.tripMemberJpaRepository.countByTripIdAndStatus(tripId, TripMemberStatus.ACTIVE);
        if (approvedMemberCount == 0) {
            approvedMemberCount = 1; // avoid div by zero, fallback
        }

        java.math.BigDecimal perPersonAmount = BigDecimal.ZERO;
        if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
            perPersonAmount = totalSpent.divide(BigDecimal.valueOf(approvedMemberCount), 2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal myPaid = contributions.getOrDefault(currentUserId, BigDecimal.ZERO);
        BigDecimal myBalance = myPaid.subtract(perPersonAmount);

        List<TripExpenseContributionResponse> contributionResponses = contributions.entrySet().stream()
                .map(e -> {
                    BigDecimal amount = e.getValue();
                    BigDecimal percentage = BigDecimal.ZERO;
                    if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount.divide(totalSpent, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    }
                    Long userId = e.getKey();
                    edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity user = this.tripService.findUser(userId);
                    return new TripExpenseContributionResponse(
                            userId,
                            displayName(user),
                            user.getAvatarUrl(),
                            amount,
                            percentage);
                })
                .toList();

        List<TripExpenseTransactionResponse> transactionResponses = expenses.stream()
                .map(expense -> new TripExpenseTransactionResponse(
                        expense.getId(),
                        expense.getTitle(),
                        expense.getCategory(),
                        expense.getPaidBy().getId(),
                        displayName(expense.getPaidBy()),
                        expense.getAmount(),
                        expense.getExpenseDate()))
                .toList();

        return new TripExpenseResponse(
                new TripExpenseSummaryResponse(totalSpent, perPersonAmount, myBalance),
                contributionResponses,
                transactionResponses);
    }

    @Transactional
    public TripExpenseTransactionResponse addExpense(Long tripId, Long currentUserId, CreateTripExpenseRequest request) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        UserEntity paidBy = this.tripService.findUser(request.paidByUserId());
        this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, request.paidByUserId())
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenTripActionException("Paid-by user must be an active member"));

        TripExpenseEntity saved = this.tripExpenseJpaRepository.save(TripExpenseEntity.builder()
                .trip(trip)
                .title(request.title().trim())
                .category(request.category())
                .paidBy(paidBy)
                .amount(request.amount())
                .build());

        this.tripActivityLogService.log(trip, this.tripService.findUser(currentUserId), "ADD_EXPENSE", "EXPENSE", saved.getId(), "expense added");
        return new TripExpenseTransactionResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCategory(),
                paidBy.getId(),
                displayName(paidBy),
                saved.getAmount(),
                saved.getExpenseDate());
    }

    @Transactional
    public TripExpenseTransactionResponse updateExpense(
            Long tripId,
            Long expenseId,
            Long currentUserId,
            UpdateTripExpenseRequest request) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripExpenseEntity expense = this.tripExpenseJpaRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        UserEntity paidBy = this.tripService.findUser(request.paidByUserId());
        this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, request.paidByUserId())
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenTripActionException("Paid-by user must be an active member"));

        expense.setTitle(request.title().trim());
        expense.setCategory(request.category());
        expense.setAmount(request.amount());
        expense.setPaidBy(paidBy);
        TripExpenseEntity saved = this.tripExpenseJpaRepository.save(expense);

        this.tripActivityLogService.log(saved.getTrip(), this.tripService.findUser(currentUserId), "UPDATE_EXPENSE", "EXPENSE", saved.getId(), "expense updated");
        return new TripExpenseTransactionResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCategory(),
                paidBy.getId(),
                displayName(paidBy),
                saved.getAmount(),
                saved.getExpenseDate());
    }

    @Transactional
    public void deleteExpense(Long tripId, Long expenseId, Long currentUserId) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripExpenseEntity expense = this.tripExpenseJpaRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        this.tripExpenseJpaRepository.delete(expense);
        this.tripActivityLogService.log(expense.getTrip(), this.tripService.findUser(currentUserId), "DELETE_EXPENSE", "EXPENSE", expenseId, "expense deleted");
    }

    private String displayName(UserEntity user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }
}