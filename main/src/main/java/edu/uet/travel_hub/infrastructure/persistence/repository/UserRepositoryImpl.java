package edu.uet.travel_hub.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserJpaEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.UserPersistenceMapper;

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
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = this.userJpaRepository.save(entity);
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
        UserJpaEntity entity = this.userJpaRepository.findById(id).get();
        entity.setRefreshToken(refreshToken);
        this.userJpaRepository.save(entity);
    }
}
