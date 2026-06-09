package edu.uet.travel_hub.infrastructure.config;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import edu.uet.travel_hub.infrastructure.security.JwtSecretKeyFactory;
import edu.uet.travel_hub.infrastructure.security.JwtTokenProvider;

@Configuration
public class JwtConfig {
    @Value("${secret.key}")
    private String secretKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = JwtSecretKeyFactory.create(secretKey);
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(JwtSecretKeyFactory.create(secretKey))
                .macAlgorithm(JwtTokenProvider.JWT_ALGORITHM)
                .build();
    }
}
