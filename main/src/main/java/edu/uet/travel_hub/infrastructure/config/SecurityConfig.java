package edu.uet.travel_hub.infrastructure.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

import edu.uet.travel_hub.domain.enums.Role;
import edu.uet.travel_hub.infrastructure.security.JwtAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
    @Value("${secret.key}")
    private String secretKey;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    // Define password encoder algorithm
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(Customizer.withDefaults()) // Enable CORS
                .authorizeHttpRequests(
                        (authz) -> authz
                                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                                .requestMatchers("/api/auth/logout").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/posts/*").authenticated()
                                .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/*/followers").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/*/following").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/users/*/follow").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/places/recommendations").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/places", "/api/places/*",
                                        "/api/places/*/reviews").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/admin/places")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.GET, "/api/admin/places/*")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.PUT, "/api/admin/places/*")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.PUT, "/api/places/*/review").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/users/me/place-view-history").authenticated()
                                .anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(MacAlgorithm.HS256).build();

        return (token) -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                throw e;
            }
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(secretKey).decode();
        return new SecretKeySpec(keyBytes, MacAlgorithm.HS256.getName());
    }
}
