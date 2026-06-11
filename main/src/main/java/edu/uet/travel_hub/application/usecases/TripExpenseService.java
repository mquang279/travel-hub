package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripExpenseRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripExpenseRequest;
import edu.uet.travel_hub.application.dto.response.TripExpenseContributionResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseSplitShareResponse;
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
        BigDecimal myOwed = expenses.stream()
                .flatMap(expense -> expense.getSplits().stream())
                .filter(split -> split.getUser().getId().equals(currentUserId))
                .map(ExpenseSplitEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal myBalance = myPaid.subtract(myOwed);

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
                        resolveSplitType(expense.getSplitType()),
                        expense.getSplits().stream()
                                .map(split -> split.getUser().getId())
                                .toList(),
                        toSplitShareResponses(expense)))
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
        Map<Long, BigDecimal> splitAmounts = normalizeSplitAmounts(
                tripId,
                amount,
                request.splitType(),
                request.splitUserIds(),
                request.splitShares());
        String splitType = resolveSplitType(request.splitType());

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
                .splitType(splitType)
                .expenseDate(resolveExpenseDate(request.expenseDate()))
                .build());
        replaceSplits(saved, splitAmounts);

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
                splitType,
                new ArrayList<>(splitAmounts.keySet()),
                toSplitShareResponses(splitAmounts));
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
        Map<Long, BigDecimal> splitAmounts = normalizeSplitAmounts(
                tripId,
                amount,
                request.splitType(),
                request.splitUserIds(),
                request.splitShares());
        String splitType = resolveSplitType(request.splitType());

        expense.setTitle(request.title().trim());
        expense.setCategory(request.category());
        expense.setAmount(amount);
        expense.setPaidBy(paidBy);
        expense.setNote(normalizeOptional(request.note()));
        expense.setSource(resolveSource(request.source()));
        expense.setRawOcrText(normalizeOptional(request.rawOcrText()));
        expense.setProofImageUrl(normalizeOptional(request.proofImageUrl()));
        expense.setSplitType(splitType);
        expense.setExpenseDate(resolveExpenseDate(request.expenseDate()));
        TripExpenseEntity saved = this.tripExpenseJpaRepository.save(expense);
        replaceSplits(saved, splitAmounts);

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
                splitType,
                new ArrayList<>(splitAmounts.keySet()),
                toSplitShareResponses(splitAmounts));
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

    private Map<Long, BigDecimal> normalizeSplitAmounts(
            Long tripId,
            BigDecimal amount,
            String splitType,
            List<Long> requestedSplitUserIds,
            List<edu.uet.travel_hub.application.dto.request.TripExpenseSplitShareRequest> requestedSplitShares) {
        if (isCustomSplit(splitType)) {
            return normalizeCustomSplitAmounts(tripId, amount, requestedSplitShares);
        }
        List<Long> splitUserIds = normalizeSplitUserIds(tripId, requestedSplitUserIds);
        return this.expenseSplitCalculator.splitEqual(normalizeMoney(amount), splitUserIds);
    }

    private Map<Long, BigDecimal> normalizeCustomSplitAmounts(
            Long tripId,
            BigDecimal amount,
            List<edu.uet.travel_hub.application.dto.request.TripExpenseSplitShareRequest> requestedSplitShares) {
        if (requestedSplitShares == null || requestedSplitShares.isEmpty()) {
            throw new IllegalArgumentException("Split shares must not be empty for custom split");
        }
        Map<Long, BigDecimal> splitAmounts = new LinkedHashMap<>();
        for (var share : requestedSplitShares) {
            if (share == null || share.userId() == null) {
                throw new IllegalArgumentException("Split share user must not be empty");
            }
            requireActiveMember(tripId, share.userId(), "Split user must be an active member");
            BigDecimal shareAmount = normalizeMoney(share.amount());
            if (shareAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Split share amount must be greater than zero");
            }
            splitAmounts.merge(share.userId(), shareAmount, BigDecimal::add);
        }
        BigDecimal totalSplitAmount = splitAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSplitAmount.compareTo(normalizeMoney(amount)) != 0) {
            throw new IllegalArgumentException("Split share amounts must equal expense amount");
        }
        return splitAmounts;
    }

    private TripMemberEntity requireActiveMember(Long tripId, Long userId, String message) {
        return this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, userId)
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenTripActionException(message));
    }

    private void replaceSplits(TripExpenseEntity expense, Map<Long, BigDecimal> splitAmounts) {
        this.expenseSplitJpaRepository.deleteByExpenseId(expense.getId());
        this.expenseSplitJpaRepository.flush();
        List<ExpenseSplitEntity> splits = new ArrayList<>();
        splitAmounts.forEach((userId, splitAmount) -> splits.add(ExpenseSplitEntity.builder()
                .expense(expense)
                .user(this.tripService.findUser(userId))
                .amount(splitAmount)
                .build()));
        this.expenseSplitJpaRepository.saveAll(splits);
    }

    private List<TripExpenseSplitShareResponse> toSplitShareResponses(TripExpenseEntity expense) {
        return expense.getSplits().stream()
                .map(split -> new TripExpenseSplitShareResponse(split.getUser().getId(), split.getAmount()))
                .toList();
    }

    private List<TripExpenseSplitShareResponse> toSplitShareResponses(Map<Long, BigDecimal> splitAmounts) {
        return splitAmounts.entrySet().stream()
                .map(entry -> new TripExpenseSplitShareResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isCustomSplit(String splitType) {
        return splitType != null
                && (splitType.equalsIgnoreCase("CUSTOM")
                || splitType.equalsIgnoreCase("SPECIFIC")
                || splitType.equalsIgnoreCase("AMOUNT"));
    }

    private String resolveSplitType(String splitType) {
        return isCustomSplit(splitType) ? "CUSTOM" : "EQUAL";
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
