package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.port.out.SavedPostRepository;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.SavedPostEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PostJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.SavedPostJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Repository
public class SavedPostRepositoryImpl implements SavedPostRepository {
    private final SavedPostJpaRepository savedPostJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PostJpaRepository postJpaRepository;

    public SavedPostRepositoryImpl(SavedPostJpaRepository savedPostJpaRepository, UserJpaRepository userJpaRepository,
            PostJpaRepository postJpaRepository) {
        this.savedPostJpaRepository = savedPostJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.postJpaRepository = postJpaRepository;
    }

    @Override
    public void save(Long userId, Long postId) {
        UserEntity userEntity = this.userJpaRepository.findById(userId).get();
        PostEntity postEntity = this.postJpaRepository.findById(postId).get();
        SavedPostEntity savedPostEntity = SavedPostEntity.builder()
                .user(userEntity)
                .post(postEntity)
                .build();
        this.savedPostJpaRepository.save(savedPostEntity);
    }

    @Override
    public void delete(Long userId, Long postId) {
        this.savedPostJpaRepository.findByUserIdAndPostId(userId, postId)
                .ifPresent(this.savedPostJpaRepository::delete);
    }

    @Override
    public boolean exists(Long userId, Long postId) {
        return this.savedPostJpaRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public long countByPostId(Long postId) {
        return this.savedPostJpaRepository.countByPostId(postId);
    }

    @Override
    public Set<Long> findSavedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(this.savedPostJpaRepository.findSavedPostIds(userId, postIds));
    }
}
