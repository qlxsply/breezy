package com.corwin.framework.web.advice;

import com.corwin.framework.error.ErrorCode;
import java.util.Optional;
import org.springframework.http.HttpStatusCode;

/**
 * Resolves an HTTP status for business error codes owned by downstream modules.
 *
 * @author Corwin 2026/8/25
 */
public interface ErrorHttpStatusResolver {

  Optional<HttpStatusCode> resolve(ErrorCode errorCode);
}
