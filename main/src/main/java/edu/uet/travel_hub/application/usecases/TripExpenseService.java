package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripExpenseRequest;
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

        List<TripExpenseContributionResponse> contributionResponses = expenses.stream()
                .map(TripExpenseEntity::getPaidBy)
                .distinct()
                .map(user -> new TripExpenseContributionResponse(
                        user.getId(),
                        displayName(user),
                        user.getAvatarUrl(),
                        contributions.getOrDefault(user.getId(), BigDecimal.ZERO)))
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
                new TripExpenseSummaryResponse(totalSpent, trip.getBudgetMin(), trip.getBudgetMax()),
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

    private String displayName(UserEntity user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return user.getUsername();
    }
}