package com.corwin.system.auth.infrastructure.web;

import com.corwin.framework.error.ErrorCode;
import com.corwin.framework.web.advice.ErrorHttpStatusResolver;
import com.corwin.system.auth.application.error.AuthError;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Maps authentication and authorization errors to their HTTP semantics.
 *
 * @author Corwin 2026/8/25
 */
@Component
public class AuthErrorHttpStatusResolver implements ErrorHttpStatusResolver {

    @Override
    public Optional<HttpStatusCode> resolve(ErrorCode errorCode) {
        if (!(errorCode instanceof AuthError authError)) {
            return Optional.empty();
        }
        return Optional.of(resolve(authError));
    }

    public static HttpStatus resolve(AuthError error) {
        return switch (error) {
            case UNAUTHENTICATED, INVALID_TOKEN, TOKEN_EXPIRED, TOKEN_REVOKED, BAD_CREDENTIALS ->
                    HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, USER_DISABLED -> HttpStatus.FORBIDDEN;
            case PERMISSION_DECLARATION_MISSING -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

}
