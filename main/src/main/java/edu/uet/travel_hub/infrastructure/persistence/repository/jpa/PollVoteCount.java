package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

public interface PollVoteCount {
    Long getPollId();

    Long getCount();
}
