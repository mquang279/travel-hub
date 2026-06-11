package edu.uet.travel_hub.application.usecases;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.uet.travel_hub.application.dto.response.PaymentProofResponse;
import edu.uet.travel_hub.application.dto.response.SettlementReceiverResponse;
import edu.uet.travel_hub.application.dto.response.SettlementResponse;
import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.out.FileStorage;
import edu.uet.travel_hub.domain.enums.SettlementStatus;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.BankAccountEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.ExpenseSplitEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.PaymentProofEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.SettlementEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ExpenseSplitJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PaymentProofJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.SettlementJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;

@Service
public class SettlementService {
    private final SettlementJpaRepository settlementJpaRepository;
    private final ExpenseSplitJpaRepository expenseSplitJpaRepository;
    private final PaymentProofJpaRepository paymentProofJpaRepository;
    private final BankAccountJpaRepository bankAccountJpaRepository;
    private final TripService tripService;
    private final FileStorage fileStorage;
    private final TripJpaRepository tripJpaRepository;

    public SettlementService(
            SettlementJpaRepository settlementJpaRepository,
            ExpenseSplitJpaRepository expenseSplitJpaRepository,
            PaymentProofJpaRepository paymentProofJpaRepository,
            BankAccountJpaRepository bankAccountJpaRepository,
            TripService tripService,
            FileStorage fileStorage,
            TripJpaRepository tripJpaRepository) {
        this.settlementJpaRepository = settlementJpaRepository;
        this.expenseSplitJpaRepository = expenseSplitJpaRepository;
        this.paymentProofJpaRepository = paymentProofJpaRepository;
        this.bankAccountJpaRepository = bankAccountJpaRepository;
        this.tripService = tripService;
        this.fileStorage = fileStorage;
        this.tripJpaRepository = tripJpaRepository;
    }

    @Transactional
    public List<SettlementResponse> finishTrip(Long tripId, Long currentUserId) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        trip.setStatus(edu.uet.travel_hub.domain.enums.TripStatus.COMPLETED);
        TripEntity savedTrip = this.tripJpaRepository.save(trip);
        return generateSettlements(savedTrip);
    }

    @Transactional
    public List<SettlementResponse> generateSettlements(TripEntity trip) {
        if (this.settlementJpaRepository.existsByTripId(trip.getId())) {
            return listSettlements(trip.getId(), trip.getLeader().getId());
        }

        List<SettlementEntity> created = new ArrayList<>();
        Long leaderUserId = trip.getLeader().getId();
        Map<Long, BigDecimal> memberOwedAmounts = calculateMemberOwedAmounts(trip.getId(), leaderUserId);
        for (Map.Entry<Long, BigDecimal> entry : memberOwedAmounts.entrySet()) {
            Long memberUserId = entry.getKey();
            BigDecimal amount = entry.getValue();
            if (amount.compareTo(BigDecimal.ZERO) > 0 && !memberUserId.equals(leaderUserId)) {
                SettlementEntity settlement = this.settlementJpaRepository.save(SettlementEntity.builder()
                        .trip(trip)
                        .fromUser(this.tripService.findUser(memberUserId))
                        .toUser(trip.getLeader())
                        .amount(amount)
                        .status(initialStatus(leaderUserId))
                        .build());
                settlement.setTransferContent("TRIP-" + trip.getId() + "-SETTLEMENT-" + settlement.getId());
                created.add(this.settlementJpaRepository.save(settlement));
            }
        }
        return created.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> listSettlements(Long tripId, Long currentUserId) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        return this.settlementJpaRepository.findByTripIdOrderByIdAsc(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long settlementId, Long currentUserId) {
        SettlementEntity settlement = findSettlementForParticipant(settlementId, currentUserId);
        return toResponse(settlement);
    }

    @Transactional
    public PaymentProofResponse uploadProof(Long settlementId, Long currentUserId, MultipartFile file, String note) {
        SettlementEntity settlement = findSettlementForParticipant(settlementId, currentUserId);
        if (!settlement.getFromUser().getId().equals(currentUserId)) {
            throw new ForbiddenTripActionException("Only payer can upload proof");
        }
        if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
            throw new IllegalStateException("Settlement already confirmed");
        }
        edu.uet.travel_hub.domain.model.UploadModel uploaded = this.fileStorage.upload("payment-proofs", currentUserId, file);
        PaymentProofEntity proof = this.paymentProofJpaRepository.save(PaymentProofEntity.builder()
                .settlement(settlement)
                .uploadedBy(settlement.getFromUser())
                .imageUrl(uploaded.getUrl())
                .note(note == null ? null : note.trim())
                .build());
        settlement.setStatus(SettlementStatus.PROOF_UPLOADED);
        this.settlementJpaRepository.save(settlement);
        return new PaymentProofResponse(
                proof.getId(),
                settlement.getId(),
                currentUserId,
                proof.getImageUrl(),
                proof.getNote(),
                proof.getUploadedAt());
    }

    @Transactional
    public SettlementResponse confirm(Long settlementId, Long currentUserId) {
        SettlementEntity settlement = requireReceiverWithUploadedProof(settlementId, currentUserId);
        settlement.setStatus(SettlementStatus.CONFIRMED);
        return toResponse(this.settlementJpaRepository.save(settlement));
    }

    @Transactional
    public SettlementResponse reject(Long settlementId, Long currentUserId) {
        SettlementEntity settlement = requireReceiverWithUploadedProof(settlementId, currentUserId);
        settlement.setStatus(SettlementStatus.REJECTED);
        return toResponse(this.settlementJpaRepository.save(settlement));
    }

    private Map<Long, BigDecimal> calculateMemberOwedAmounts(Long tripId, Long leaderUserId) {
        List<ExpenseSplitEntity> splits = this.expenseSplitJpaRepository.findByExpenseTripId(tripId);
        Map<Long, BigDecimal> owedAmounts = new LinkedHashMap<>();
        for (ExpenseSplitEntity split : splits) {
            Long userId = split.getUser().getId();
            if (!userId.equals(leaderUserId)) {
                owedAmounts.merge(userId, split.getAmount(), BigDecimal::add);
            }
        }
        return owedAmounts;
    }

    private SettlementEntity findSettlementForParticipant(Long settlementId, Long currentUserId) {
        SettlementEntity settlement = this.settlementJpaRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));
        this.tripService.requireActiveMemberTrip(settlement.getTrip().getId(), currentUserId);
        return settlement;
    }

    private SettlementEntity requireReceiverWithUploadedProof(Long settlementId, Long currentUserId) {
        SettlementEntity settlement = findSettlementForParticipant(settlementId, currentUserId);
        if (!settlement.getToUser().getId().equals(currentUserId)) {
            throw new ForbiddenTripActionException("Only receiver can confirm settlement");
        }
        if (settlement.getStatus() != SettlementStatus.PROOF_UPLOADED) {
            throw new IllegalStateException("Settlement proof must be uploaded first");
        }
        return settlement;
    }

    private SettlementStatus initialStatus(Long receiverUserId) {
        return this.bankAccountJpaRepository.findFirstByUserIdAndIsDefaultTrue(receiverUserId).isPresent()
                ? SettlementStatus.QR_READY
                : SettlementStatus.PENDING;
    }

    private SettlementResponse toResponse(SettlementEntity settlement) {
        BankAccountEntity bankAccount = this.bankAccountJpaRepository
                .findFirstByUserIdAndIsDefaultTrue(settlement.getToUser().getId())
                .orElse(null);
        SettlementReceiverResponse receiver = new SettlementReceiverResponse(
                settlement.getToUser().getId(),
                bankAccount == null ? null : bankAccount.getBankCode(),
                bankAccount == null ? null : bankAccount.getBankName(),
                bankAccount == null ? null : bankAccount.getAccountNumber(),
                bankAccount == null ? null : bankAccount.getAccountName());
        return new SettlementResponse(
                settlement.getId(),
                settlement.getTrip().getId(),
                settlement.getFromUser().getId(),
                settlement.getToUser().getId(),
                settlement.getAmount(),
                settlement.getStatus(),
                settlement.getTransferContent(),
                receiver);
    }
}
