package edu.uet.travel_hub.application.port.out;

import edu.uet.travel_hub.domain.model.UserModel;

public interface TokenProvider {
    String generateAccessToken(UserModel user);
    String generateRefreshToken(UserModel user);
    boolean isValidRefreshToken(String refreshToken);
} 
