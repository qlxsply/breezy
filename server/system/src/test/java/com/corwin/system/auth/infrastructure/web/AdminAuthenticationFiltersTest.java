package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.json.Json;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.infrastructure.security.AuthPrincipalAuthenticator;
import com.corwin.system.auth.infrastructure.security.OpaqueTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Corwin 2026/8/20
 */
class AdminAuthenticationFiltersTest {

    @BeforeAll
    static void initializeJson() {
        new Json(new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void rejectsCookieAuthenticatedMutationWithoutMatchingCsrfToken() throws Exception {
        AdminAuthCookieService cookieService = mock(AdminAuthCookieService.class);
        when(cookieService.readSession(org.mockito.ArgumentMatchers.any())).thenReturn("session");
        when(cookieService.readCsrf(org.mockito.ArgumentMatchers.any())).thenReturn("csrf-token");
        AdminCsrfFilter filter = new AdminCsrfFilter(cookieService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void acceptsCookieAuthenticatedMutationWithMatchingCsrfToken() throws Exception {
        AdminAuthCookieService cookieService = mock(AdminAuthCookieService.class);
        when(cookieService.readSession(org.mockito.ArgumentMatchers.any())).thenReturn("session");
        when(cookieService.readCsrf(org.mockito.ArgumentMatchers.any())).thenReturn("csrf-token");
        AdminCsrfFilter filter = new AdminCsrfFilter(cookieService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        request.addHeader(AdminAuthCookieService.CSRF_HEADER, "csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void keepsBearerAuthenticationIndependentFromAdminCookie() throws Exception {
        AuthPrincipalAuthenticator authenticator = mock(AuthPrincipalAuthenticator.class);
        OpaqueTokenService opaqueTokenService = mock(OpaqueTokenService.class);
        AdminAuthCookieService cookieService = mock(AdminAuthCookieService.class);
        AuthPrincipal user = new AuthPrincipal(7L, "user", UserType.USER, false, Set.of());
        when(authenticator.authenticateUserToken("header.jwt.token")).thenReturn(user);
        when(cookieService.readSession(org.mockito.ArgumentMatchers.any())).thenReturn("admin-session");
        AuthenticationFilter filter = new AuthenticationFilter(authenticator, opaqueTokenService, cookieService,
                Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader(HttpHeaderNames.AUTHORIZATION, "Bearer header.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(authenticator).authenticateUserToken("header.jwt.token");
        verify(authenticator, never()).authenticateAdminToken("admin-session");
        verify(cookieService, never()).clearSession(response);
    }
}
