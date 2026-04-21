package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.UserPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserPersistenceMapper mapper) {
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public UserModel save(UserModel user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = this.userJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserModel> findById(Long id) {
        return this.userJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserModel> findByEmail(String email) {
        return this.userJpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public void updateRefreshToken(Long id, String refreshToken) {
        UserEntity entity = this.userJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        entity.setRefreshToken(refreshToken);
        this.userJpaRepository.save(entity);
    }

    @Override
    public void incrementFollowing(Long id) {
        this.userJpaRepository.incrementFollowing(id);
    }

    @Override
    public void decrementFollowing(Long id) {
        this.userJpaRepository.decrementFollowing(id);
    }

    @Override
    public void incrementFollowers(Long id) {
        this.userJpaRepository.incrementFollowers(id);
    }

    @Override
    public void decrementFollowers(Long id) {
        this.userJpaRepository.decrementFollowers(id);
    }
}
