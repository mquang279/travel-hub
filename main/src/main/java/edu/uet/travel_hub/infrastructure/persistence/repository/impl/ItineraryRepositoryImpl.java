package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.port.out.ItineraryRepository;
import edu.uet.travel_hub.domain.model.ItineraryDetailRowModel;
import edu.uet.travel_hub.domain.model.ItineraryModel;
import edu.uet.travel_hub.domain.model.ItinerarySummaryModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.ItineraryPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ItineraryJpaRepository;

@Repository
public class ItineraryRepositoryImpl implements ItineraryRepository {
    private final ItineraryJpaRepository itineraryJpaRepository;
    private final ItineraryPersistenceMapper mapper;

    public ItineraryRepositoryImpl(ItineraryJpaRepository itineraryJpaRepository, ItineraryPersistenceMapper mapper) {
        this.itineraryJpaRepository = itineraryJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItinerarySummaryModel> findSummariesByOwnerId(Long ownerId) {
        return this.itineraryJpaRepository.findSummariesByOwnerId(ownerId)
                .stream()
                .map(this.mapper::toSummaryDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItineraryModel> findByIdAndOwnerId(Long id, Long ownerId) {
        return this.itineraryJpaRepository.findByIdAndOwnerId(id, ownerId).map(this.mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItineraryModel> findByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName) {
        return this.itineraryJpaRepository.findByOwnerIdAndGroupNameIgnoreCase(ownerId, groupName)
                .map(this.mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName) {
        return this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCase(ownerId, groupName);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(Long ownerId, String groupName, Long id) {
        return this.itineraryJpaRepository.existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(ownerId, groupName, id);
    }

    @Override
    @Transactional
    public ItineraryModel saveAndFlush(ItineraryModel itinerary) {
        ItineraryEntity saved = this.itineraryJpaRepository.saveAndFlush(this.mapper.toEntity(itinerary));
        return this.mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void delete(ItineraryModel itinerary) {
        this.itineraryJpaRepository.delete(this.mapper.toEntity(itinerary));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItineraryDetailRowModel> findDetailRowsByIdAndOwnerId(Long id, Long ownerId) {
        return this.itineraryJpaRepository.findDetailRowsByIdAndOwnerId(id, ownerId)
                .stream()
                .map(this.mapper::toDetailRowDomain)
                .toList();
    }
}
