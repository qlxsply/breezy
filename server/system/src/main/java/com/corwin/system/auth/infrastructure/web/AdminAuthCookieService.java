package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.util.HighDate;
import com.corwin.system.auth.application.service.AuthConfigService;
import com.corwin.system.auth.infrastructure.security.OpaqueTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * @author Corwin 2026/8/20
 */
@Component
@RequiredArgsConstructor
public class AdminAuthCookieService {

    public static final String SESSION_COOKIE = "__Host-breezy-admin-session";
    public static final String CSRF_COOKIE = "__Host-breezy-admin-csrf";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final AuthConfigService authConfigService;
    private final OpaqueTokenService opaqueTokenService;

    public void writeSession(HttpServletResponse response, String token, String expiresAt) {
        Instant expiration = Instant.ofEpochMilli(Long.parseLong(expiresAt));
        Duration maxAge = Duration.between(HighDate.realInstant(), expiration);
        addCookie(response, SESSION_COOKIE, token, maxAge.isNegative() ? Duration.ZERO : maxAge, true);
    }

    public void clearSession(HttpServletResponse response) {
        addCookie(response, SESSION_COOKIE, "", Duration.ZERO, true);
    }

    public void rotateCsrf(HttpServletResponse response) {
        addCookie(response, CSRF_COOKIE, opaqueTokenService.generateToken(), authConfigService.adminSessionTtl(),
                false);
    }

    public String readSession(HttpServletRequest request) {
        return readCookie(request, SESSION_COOKIE);
    }

    public String readCsrf(HttpServletRequest request) {
        return readCookie(request, CSRF_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value).httpOnly(httpOnly).secure(true).sameSite("Strict")
                .path("/").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        return Arrays.stream(cookies).filter(cookie -> name.equals(cookie.getName())).map(Cookie::getValue).findFirst()
                .orElse("").trim();
    }
}
