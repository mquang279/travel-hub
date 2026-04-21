package edu.uet.travel_hub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowModel {
    private Long id;
    private Long followerId;
    private Long followingId;
}
