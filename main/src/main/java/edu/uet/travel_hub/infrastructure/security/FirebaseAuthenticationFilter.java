package edu.uet.travel_hub.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import edu.uet.travel_hub.application.usecases.FirebaseUserSyncService;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseUserSyncService firebaseUserSyncService;
    private final JwtDecoder jwtDecoder;
    private final UserJpaRepository userJpaRepository;

    public FirebaseAuthenticationFilter(
            FirebaseAuth firebaseAuth,
            FirebaseUserSyncService firebaseUserSyncService,
            JwtDecoder jwtDecoder,
            UserJpaRepository userJpaRepository) {
        this.firebaseAuth = firebaseAuth;
        this.firebaseUserSyncService = firebaseUserSyncService;
        this.jwtDecoder = jwtDecoder;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("/api/auth/session".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authenticateJwt(token)) {
            authenticateFirebase(token);
        }

        filterChain.doFilter(request, response);
    }

    private boolean authenticateJwt(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String email = jwt.getSubject();
            if (email == null || email.isBlank()) {
                return false;
            }

            UserEntity user = userJpaRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return false;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    token,
                    List.of(new SimpleGrantedAuthority(user.getRole().getDescription())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            return false;
        }
    }

    private void authenticateFirebase(String token) {
        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token);
            FirebaseAuthenticatedUser user = firebaseUserSyncService.sync(firebaseToken);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    token,
                    List.of(new SimpleGrantedAuthority(user.role().getDescription())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (FirebaseAuthException | RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
