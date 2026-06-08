package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.enums.ExpenseSource;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.ExpenseSplitEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ExpenseSplitJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripExpenseJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;

@Service
public class TripExpenseService {
    private final TripService tripService;
    private final TripExpenseJpaRepository tripExpenseJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final TripActivityLogService tripActivityLogService;
    private final ExpenseSplitJpaRepository expenseSplitJpaRepository;
    private final ExpenseSplitCalculator expenseSplitCalculator;
    private final FileStorage fileStorage;

    public TripExpenseService(
            TripService tripService,
            TripExpenseJpaRepository tripExpenseJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            TripActivityLogService tripActivityLogService,
            ExpenseSplitJpaRepository expenseSplitJpaRepository,
            ExpenseSplitCalculator expenseSplitCalculator,
            FileStorage fileStorage) {
        this.tripService = tripService;
        this.tripExpenseJpaRepository = tripExpenseJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
        this.expenseSplitJpaRepository = expenseSplitJpaRepository;
        this.expenseSplitCalculator = expenseSplitCalculator;
        this.fileStorage = fileStorage;
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
                        expense.getExpenseDate(),
                        this.fileStorage.resolvePublicUrl(expense.getProofImageUrl()),
                        expense.getSplits().stream()
                                .map(split -> split.getUser().getId())
                                .toList()))
                .toList();

        return new TripExpenseResponse(
                new TripExpenseSummaryResponse(totalSpent, perPersonAmount, myBalance),
                contributionResponses,
                transactionResponses);
    }

    @Transactional
    public TripExpenseTransactionResponse addExpense(Long tripId, Long currentUserId, CreateTripExpenseRequest request) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        requireTripNotCompleted(trip);
        UserEntity paidBy = this.tripService.findUser(request.paidByUserId());
        requireActiveMember(tripId, request.paidByUserId(), "Paid-by user must be an active member");
        BigDecimal amount = normalizeAmount(request.totalAmount(), request.amount());
        List<Long> splitUserIds = normalizeSplitUserIds(tripId, request.splitUserIds());

        TripExpenseEntity saved = this.tripExpenseJpaRepository.save(TripExpenseEntity.builder()
                .trip(trip)
                .title(request.title().trim())
                .category(request.category())
                .paidBy(paidBy)
                .amount(amount)
                .note(normalizeOptional(request.note()))
                .source(resolveSource(request.source()))
                .rawOcrText(normalizeOptional(request.rawOcrText()))
                .proofImageUrl(normalizeOptional(request.proofImageUrl()))
                .expenseDate(resolveExpenseDate(request.expenseDate()))
                .build());
        replaceSplits(saved, splitUserIds, amount);

        this.tripActivityLogService.log(trip, this.tripService.findUser(currentUserId), "ADD_EXPENSE", "EXPENSE", saved.getId(), "expense added");
        return new TripExpenseTransactionResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCategory(),
                paidBy.getId(),
                displayName(paidBy),
                saved.getAmount(),
                saved.getExpenseDate(),
                this.fileStorage.resolvePublicUrl(saved.getProofImageUrl()),
                splitUserIds);
    }

    @Transactional
    public TripExpenseTransactionResponse updateExpense(
            Long tripId,
            Long expenseId,
            Long currentUserId,
            UpdateTripExpenseRequest request) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripEntity trip = this.tripService.findTrip(tripId);
        requireTripNotCompleted(trip);
        TripExpenseEntity expense = this.tripExpenseJpaRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        UserEntity paidBy = this.tripService.findUser(request.paidByUserId());
        requireActiveMember(tripId, request.paidByUserId(), "Paid-by user must be an active member");
        BigDecimal amount = normalizeAmount(request.totalAmount(), request.amount());
        List<Long> splitUserIds = normalizeSplitUserIds(tripId, request.splitUserIds());

        expense.setTitle(request.title().trim());
        expense.setCategory(request.category());
        expense.setAmount(amount);
        expense.setPaidBy(paidBy);
        expense.setNote(normalizeOptional(request.note()));
        expense.setSource(resolveSource(request.source()));
        expense.setRawOcrText(normalizeOptional(request.rawOcrText()));
        expense.setProofImageUrl(normalizeOptional(request.proofImageUrl()));
        expense.setExpenseDate(resolveExpenseDate(request.expenseDate()));
        TripExpenseEntity saved = this.tripExpenseJpaRepository.save(expense);
        replaceSplits(saved, splitUserIds, amount);

        this.tripActivityLogService.log(saved.getTrip(), this.tripService.findUser(currentUserId), "UPDATE_EXPENSE", "EXPENSE", saved.getId(), "expense updated");
        return new TripExpenseTransactionResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCategory(),
                paidBy.getId(),
                displayName(paidBy),
                saved.getAmount(),
                saved.getExpenseDate(),
                this.fileStorage.resolvePublicUrl(saved.getProofImageUrl()),
                splitUserIds);
    }

    @Transactional
    public void deleteExpense(Long tripId, Long expenseId, Long currentUserId) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        requireTripNotCompleted(trip);
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

    private BigDecimal normalizeAmount(BigDecimal totalAmount, BigDecimal amount) {
        BigDecimal resolved = totalAmount != null ? totalAmount : amount;
        if (resolved == null || resolved.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }
        return resolved;
    }

    private List<Long> normalizeSplitUserIds(Long tripId, List<Long> requestedSplitUserIds) {
        List<Long> splitUserIds = requestedSplitUserIds;
        if (splitUserIds == null || splitUserIds.isEmpty()) {
            splitUserIds = this.tripMemberJpaRepository.findByTripIdAndStatus(tripId, TripMemberStatus.ACTIVE)
                    .stream()
                    .map(member -> member.getUser().getId())
                    .toList();
        }
        if (splitUserIds.isEmpty()) {
            throw new IllegalArgumentException("Split users must not be empty");
        }
        splitUserIds.forEach(userId -> requireActiveMember(tripId, userId, "Split user must be an active member"));
        return splitUserIds.stream().distinct().toList();
    }

    private TripMemberEntity requireActiveMember(Long tripId, Long userId, String message) {
        return this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, userId)
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenTripActionException(message));
    }

    private void replaceSplits(TripExpenseEntity expense, List<Long> splitUserIds, BigDecimal amount) {
        this.expenseSplitJpaRepository.deleteByExpenseId(expense.getId());
        Map<Long, BigDecimal> splitAmounts = this.expenseSplitCalculator.splitEqual(amount, splitUserIds);
        List<ExpenseSplitEntity> splits = new ArrayList<>();
        splitAmounts.forEach((userId, splitAmount) -> splits.add(ExpenseSplitEntity.builder()
                .expense(expense)
                .user(this.tripService.findUser(userId))
                .amount(splitAmount)
                .build()));
        this.expenseSplitJpaRepository.saveAll(splits);
    }

    private void requireTripNotCompleted(TripEntity trip) {
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new IllegalStateException("Trip is completed");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ExpenseSource resolveSource(ExpenseSource source) {
        return source == null ? ExpenseSource.MANUAL : source;
    }

    private Instant resolveExpenseDate(String expenseDate) {
        if (expenseDate == null || expenseDate.isBlank()) {
            return Instant.now();
        }
        return runCatchingLocalDate(expenseDate.trim())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private LocalDate runCatchingLocalDate(String value) {
        try {
            int timeSeparatorIndex = value.indexOf('T');
            String dateValue = timeSeparatorIndex >= 0 ? value.substring(0, timeSeparatorIndex) : value;
            return LocalDate.parse(dateValue);
        } catch (RuntimeException ex) {
            return LocalDate.now();
        }
    }
}
