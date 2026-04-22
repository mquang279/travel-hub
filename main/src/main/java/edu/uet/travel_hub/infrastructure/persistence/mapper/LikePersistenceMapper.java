package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.LikeModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.LikeEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PostJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class LikePersistenceMapper {
    private final UserJpaRepository userJpaRepository;
    private final PostJpaRepository postJpaRepository;

    public LikePersistenceMapper(UserJpaRepository userJpaRepository, PostJpaRepository postJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.postJpaRepository = postJpaRepository;
    }

    public LikeModel toModel(LikeEntity likeEntity) {
        return new LikeModel(likeEntity.getUser().getId(), likeEntity.getPost().getId());
    }

    public LikeEntity toEntity(LikeModel likeModel) {
        UserEntity userEntity = this.userJpaRepository.findById(likeModel.userId()).get();
        PostEntity postEntity = this.postJpaRepository.findById(likeModel.postId()).get();
        return LikeEntity.builder().post(postEntity).user(userEntity).build();
    }
}
