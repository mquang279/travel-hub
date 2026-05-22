package edu.uet.travel_hub.domain.mapper;

import java.util.Optional;

import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripRole;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;

public final class TripRoleMapper {

    private TripRoleMapper() {}

    public static TripRole fromMemberRole(TripMemberRole memberRole) {
        if (memberRole == null) return TripRole.NON_MEMBER;
        if (memberRole == TripMemberRole.LEADER) return TripRole.LEADER;
        return TripRole.MEMBER;
    }

    public static TripRole determineRole(Optional<TripMemberEntity> memberOpt, boolean isPending) {
        if (memberOpt != null && memberOpt.isPresent()) {
            return fromMemberRole(memberOpt.get().getRole());
        }
        return isPending ? TripRole.PENDING : TripRole.NON_MEMBER;
    }
}
