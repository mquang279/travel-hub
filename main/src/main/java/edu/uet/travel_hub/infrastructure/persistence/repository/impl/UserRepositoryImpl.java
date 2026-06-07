package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.enums.Role;
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
    @Transactional
    public UserModel save(UserModel user) {
        UserEntity entity = user.getId() == null
                ? mapper.toEntity(user)
                : this.userJpaRepository.findById(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + user.getId()));

        entity.setUsername(user.getUsername());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setHashPassword(user.getHashPassword());
        entity.setBio(user.getBio());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setDateOfBirth(user.getDateOfBirth());
        entity.setGender(user.getGender());
        entity.setLocation(user.getLocation());
        entity.setTripType(user.getTripType());
        entity.setInterests(user.getInterests() == null ? new ArrayList<>() : new ArrayList<>(user.getInterests()));
        entity.setDestination(user.getDestination());
        entity.setOnboarded(user.isOnboarded());
        entity.setFollowersCount(user.getFollowersCount());
        entity.setFollowingCount(user.getFollowingCount());
        entity.setPostsCount(user.getPostsCount());

        UserEntity saved = this.userJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findById(Long id) {
        return this.userJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByIdWithInterests(Long id) {
        return this.userJpaRepository.findByIdWithInterests(id).map(mapper::toDomainWithInterests);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByEmail(String email) {
        return this.userJpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return this.userJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return this.userJpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return this.userJpaRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserModel> findByRefreshToken(String refreshToken) {
        return this.userJpaRepository.findByRefreshToken(refreshToken).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserModel> searchByUsername(String username, int pageNumber, int pageSize) {
        PageRequest request = PageRequest.of(pageNumber, pageSize);
        Page<UserEntity> users = this.userJpaRepository.searchByUsername(normalizeSearchTerm(username), request);
        return new PaginationResponse<>(
                users.getNumber(),
                users.getSize(),
                users.getTotalPages(),
                users.getTotalElements(),
                users.getContent().stream().map(mapper::toDomain).toList());
    }

    @Override
    @Transactional
    public void updateRefreshToken(Long id, String refreshToken) {
        UserEntity entity = this.userJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        entity.setRefreshToken(refreshToken);
        this.userJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void incrementFollowing(Long id) {
        this.userJpaRepository.incrementFollowing(id);
    }

    @Override
    @Transactional
    public void decrementFollowing(Long id) {
        this.userJpaRepository.decrementFollowing(id);
    }

    @Override
    @Transactional
    public void incrementFollowers(Long id) {
        this.userJpaRepository.incrementFollowers(id);
    }

    @Override
    @Transactional
    public void decrementFollowers(Long id) {
        this.userJpaRepository.decrementFollowers(id);
    }

    @Override
    @Transactional
    public void incrementPosts(Long id) {
        this.userJpaRepository.incrementPosts(id);
    }

    @Override
    public UserModel register(String email, String username, String name, String password, Role role) {
        UserEntity userEntity = UserEntity
                .builder()
                .email(email)
                .username(username)
                .name(name)
                .hashPassword(password)
                .role(role)
                .build();
        UserEntity saved = this.userJpaRepository.save(userEntity);
        return this.mapper.toDomain(saved);
    }

    private String normalizeSearchTerm(String term) {
        return term == null ? "" : term.trim();
    }
}
