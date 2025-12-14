package com.ees.framework.control;

import com.ees.ai.control.ControlProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Control API(`/api/control/**`)를 보호하기 위한 static token(Bearer) 인증 필터.
 */
public class ControlTokenAuthFilter extends OncePerRequestFilter {

    private final ControlProperties properties;

    public ControlTokenAuthFilter(ControlProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/control/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.getAuthToken();
        if (!StringUtils.hasText(expected)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Control auth token is not configured");
            return;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }
        String provided = auth.substring("Bearer ".length()).trim();
        if (!Objects.equals(expected, provided)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

