package edu.uet.travel_hub.infrastructure.persistence.repository.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.domain.model.FollowModel;
import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.mapper.FollowPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.mapper.UserPersistenceMapper;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.FollowJpaRepository;

@Repository
public class FollowRepositoryImpl implements FollowRepository {
	private final FollowJpaRepository followJpaRepository;
	private final FollowPersistenceMapper followMapper;
	private final UserPersistenceMapper userMapper;

	public FollowRepositoryImpl(FollowJpaRepository followJpaRepository, FollowPersistenceMapper followMapper,
			UserPersistenceMapper userMapper) {
		this.followJpaRepository = followJpaRepository;
		this.followMapper = followMapper;
		this.userMapper = userMapper;
	}

	@Override
	public Page<UserModel> findFollowers(Long targetUserId, Pageable pageable) {
		return this.followJpaRepository.findByFollowingId(targetUserId, pageable)
				.map(follow -> userMapper.toDomain(follow.getFollower()));
	}

	@Override
	public Page<UserModel> findFollowing(Long targetUserId, Pageable pageable) {
		return this.followJpaRepository.findByFollowerId(targetUserId, pageable)
				.map(follow -> userMapper.toDomain(follow.getFollowing()));
	}

	@Override
	public Optional<FollowModel> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
		return this.followJpaRepository.findByFollowerIdAndFollowingId(followerId, followingId)
				.map(followMapper::toDomain);
	}

	@Override
	public boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId) {
		return this.followJpaRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
	}

	@Override
	public FollowModel save(FollowModel followModel) {
		return followMapper.toDomain(this.followJpaRepository.save(followMapper.toEntity(followModel)));
	}

	@Override
	public void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId) {
		this.followJpaRepository.findByFollowerIdAndFollowingId(followerId, followingId)
				.ifPresent(this.followJpaRepository::delete);
	}
}
