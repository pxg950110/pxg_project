package com.maidc.common.security.filter;

import com.maidc.common.security.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * When X-User-Id header is missing (e.g. dev mode bypassing Gateway),
 * extract userId from JWT Bearer token and inject it as a request header.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class UserIdHeaderFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ORG_ID_HEADER = "X-Org-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String AUTHORIZATION = "Authorization";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String existingUserId = request.getHeader(USER_ID_HEADER);
        if (existingUserId != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put(USER_ID_HEADER, String.valueOf(userId));

            Long orgId = jwtUtils.getOrgIdFromToken(token);
            if (orgId != null) {
                extraHeaders.put(ORG_ID_HEADER, String.valueOf(orgId));
            }

            String username = jwtUtils.getUsernameFromToken(token);
            if (username != null) {
                extraHeaders.put(USERNAME_HEADER, username);
            }

            filterChain.doFilter(new HeaderInjectRequestWrapper(request, extraHeaders), response);
        } catch (Exception e) {
            log.debug("Failed to extract user info from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private static class HeaderInjectRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> extraHeaders;

        HeaderInjectRequestWrapper(HttpServletRequest request, Map<String, String> extraHeaders) {
            super(request);
            this.extraHeaders = extraHeaders;
        }

        @Override
        public String getHeader(String name) {
            if (extraHeaders.containsKey(name)) {
                return extraHeaders.get(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (extraHeaders.containsKey(name)) {
                return java.util.Collections.enumeration(java.util.Collections.singletonList(extraHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}
