package edu.uet.travel_hub.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import edu.uet.travel_hub.application.port.out.TokenProvider;
import edu.uet.travel_hub.domain.model.UserModel;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.util.Base64;

@Component
public class JwtTokenProvider implements TokenProvider {
    private final JwtEncoder jwtEncoder;

    @Value("${secret.key}")
    private String secretKey;

    @Value("${access.token.expiration.time}")
    private long accessTokenExpiration;

    @Value("${refresh.token.expiration.time}")
    private long refreshTokenExpiration;

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    public JwtTokenProvider(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public String generateAccessToken(UserModel user) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        // List<String> authorities = new ArrayList<>();
        // for (Permission permission : userDTO.getRole().getPermissions()) {
        // authorities.add(permission.getName());
        // }
        // authorities.add(userDTO.getRole().getName());

        UserClaims userClaims = UserClaims.from(user);
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getEmail())
                .claim("user", userClaims)
                // .claim("authorities", List.of(user.getRole().getDescription()))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    @Override
    public String generateRefreshToken(UserModel user) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        UserClaims userClaims = UserClaims.from(user);
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getEmail())
                .claim("user", userClaims)
                // .claim("authorities", List.of(user.getRole().getDescription()))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    @Override
    public boolean isValidRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM).build();
        try {
            jwtDecoder.decode(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = runCatchingBase64(secretKey);
        if (keyBytes.length < 32) {
            keyBytes = sha256(secretKey);
        }
        return new SecretKeySpec(keyBytes, JWT_ALGORITHM.getName());
    }

    private byte[] runCatchingBase64(String value) {
        try {
            return Base64.from(value).decode();
        } catch (Exception ignored) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to derive JWT secret key", exception);
        }
    }

}
