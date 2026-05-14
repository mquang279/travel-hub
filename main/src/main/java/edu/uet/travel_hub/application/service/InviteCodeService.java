package edu.uet.travel_hub.application.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.exception.InviteCodeGenerationException;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;

@Service
public class InviteCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final TripJpaRepository tripJpaRepository;

    public InviteCodeService(TripJpaRepository tripJpaRepository) {
        this.tripJpaRepository = tripJpaRepository;
    }

    public String generateInviteCode() {
        int attempts = 0;
        while (attempts < 5) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            boolean exists = this.tripJpaRepository.findByInviteCode(code).isPresent();
            if (!exists) {
                return code;
            }
            attempts++;
        }
        throw new InviteCodeGenerationException("Failed to generate unique invite code after 5 attempts");
    }
}
