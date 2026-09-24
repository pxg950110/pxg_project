package com.maidc.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void userId_shouldReadFromHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertEquals(42L, CurrentUser.userId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void userId_shouldReturnNullWhenMissing() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertNull(CurrentUser.userId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void userId_shouldReturnNullWhenHeaderGarbage() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "abc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        try {
            assertNull(CurrentUser.userId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
