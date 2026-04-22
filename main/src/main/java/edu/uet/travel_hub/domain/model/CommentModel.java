package edu.uet.travel_hub.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CommentModel {
    private Long id;

    private String content;

    private UserModel owner;

    private PostModel post;

    private Instant createdAt;

    private Instant updatedAt;
}
