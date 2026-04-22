package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.domain.model.LikeModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.LikeEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.mapper.LikePersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.LikeJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PostJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Repository
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;
    private final PostJpaRepository postJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final LikePersistenceMapper mapper;

    public LikeRepositoryImpl(LikeJpaRepository likeJpaRepository, PostJpaRepository postJpaRepository,
            UserJpaRepository userJpaRepository, LikePersistenceMapper mapper) {
        this.likeJpaRepository = likeJpaRepository;
        this.postJpaRepository = postJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LikeModel save(Long userId, Long postId) {
        UserEntity userEntity = this.userJpaRepository.findById(userId).get();
        PostEntity postEntity = this.postJpaRepository.findById(postId).get();
        LikeEntity likeEntity = LikeEntity
                .builder()
                .user(userEntity)
                .post(postEntity)
                .build();
        return mapper.toModel(this.likeJpaRepository.save(likeEntity));
    }

    @Override
    public void delete(Long userId, Long postId) {
        LikeEntity likeEntity = this.likeJpaRepository.findByUserIdAndPostId(userId, postId).get();
        this.likeJpaRepository.delete(likeEntity);
    }

    @Override
    public boolean exists(Long userId, Long postId) {
        return this.likeJpaRepository.existsByUserIdAndPostId(userId, postId);
    }
}
