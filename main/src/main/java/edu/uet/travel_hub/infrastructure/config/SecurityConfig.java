package edu.uet.travel_hub.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import edu.uet.travel_hub.domain.enums.Role;
import edu.uet.travel_hub.infrastructure.security.JwtAuthenticationEntryPoint;
import edu.uet.travel_hub.infrastructure.security.FirebaseAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
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
                                .requestMatchers("/api/auth/session").permitAll()
                                .requestMatchers("/api/auth/logout").authenticated()
                                .requestMatchers("/api/users/me/dashboard").authenticated()
                                .requestMatchers("/api/trips/**").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/posts/random").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/posts/*").authenticated()
                                .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/users/top-travelers").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/*/followers").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/*/following").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/users/*/follow").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/places/recommendations").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/places/featured").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/places", "/api/places/*",
                                        "/api/places/*/reviews").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/ai/travel-assistant/chat").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/admin/places")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.GET, "/api/admin/places/*")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.PUT, "/api/admin/places/*")
                                .hasAuthority(Role.ADMIN.getDescription())
                                .requestMatchers(HttpMethod.PUT, "/api/places/*/review").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/users/me/place-view-history").authenticated()
                                .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

}
