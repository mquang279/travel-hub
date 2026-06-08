package edu.uet.travel_hub.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.port.in.AuthenticationPort;

@Component
public class SpringAuthenticationAdapter implements AuthenticationPort {
    private final AuthenticationManager authenticationManager;

    public SpringAuthenticationAdapter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void authentication(String email, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                email, password);
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
