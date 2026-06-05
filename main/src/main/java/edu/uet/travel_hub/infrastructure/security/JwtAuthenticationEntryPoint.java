package edu.uet.travel_hub.infrastructure.security;

import java.io.IOException;

import edu.uet.travel_hub.interfaces.dto.response.ApiErrorResponse;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    private final ObjectMapper mapper;

    public JwtAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        this.delegate.commence(request, response, authException);
        response.setContentType("application/json;charset=UTF-8");

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Chưa xác thực",
                "Bạn cần đăng nhập hoặc token không hợp lệ.",
                request.getRequestURI());
        mapper.writeValue(response.getWriter(), errorResponse);
    }
}
