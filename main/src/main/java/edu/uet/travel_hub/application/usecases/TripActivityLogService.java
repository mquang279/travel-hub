package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripActivityLogEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripActivityLogJpaRepository;

@Service
public class TripActivityLogService {
    private final TripActivityLogJpaRepository tripActivityLogJpaRepository;

    public TripActivityLogService(TripActivityLogJpaRepository tripActivityLogJpaRepository) {
        this.tripActivityLogJpaRepository = tripActivityLogJpaRepository;
    }

    @Transactional
    public void log(TripEntity trip, UserEntity actor, String actionType, String targetType, Long targetId, String description) {
        tripActivityLogJpaRepository.save(TripActivityLogEntity.builder()
                .trip(trip)
                .actor(actor)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .build());
    }
}