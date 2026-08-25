package com.corwin.framework.web.advice;

import com.corwin.framework.config.definition.ConfigViolation;
import com.corwin.framework.config.error.ConfigValidationException;
import com.corwin.framework.config.error.ConfigVersionConflictException;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.error.SysException;
import com.corwin.framework.web.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Optional;

/**
 * Global exception handler for REST controllers.
 *
 * @author Corwin 2025/10/13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    private final List<ErrorHttpStatusResolver> errorHttpStatusResolvers;

    public GlobalExceptionAdvice(List<ErrorHttpStatusResolver> errorHttpStatusResolvers) {
        this.errorHttpStatusResolvers = errorHttpStatusResolvers;
    }

    @ExceptionHandler(ConfigValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<List<ConfigViolation>> handleConfigValidationException(ConfigValidationException ex) {
        log.warn("ConfigValidationException msg={} violations={}", ex.getMessage(), ex.violations().size());
        return ApiResponse.fail(ex.violations(), BaseError.INVALID_PARAMETER.getCode(), ex.getMessage());
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBizException(BizException ex) {
        log.warn("BizException code={} msg={}", ex.getCode(), ex.getMessage());
        HttpStatusCode status = errorHttpStatusResolvers.stream().map(resolver -> resolver.resolve(ex.getErrorCode()))
                .flatMap(Optional::stream).findFirst().orElse(HttpStatus.OK);
        return ResponseEntity.status(status).body(ApiResponse.fail(ex.getErrorCode()));
    }

    @ExceptionHandler(ConfigVersionConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConfigVersionConflictException(ConfigVersionConflictException ex) {
        log.warn("ConfigVersionConflictException code={} msg={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(ex.getErrorCode()));
    }

    @ExceptionHandler(SysException.class)
    public ApiResponse<Object> handleSysException(SysException ex) {
        log.error("SysException outwardCode={} msg={}", ex.getCode(), ex.getMessage(), ex);
        return ApiResponse.fail(ex.getOutwardCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream().findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage).orElse(BaseError.INVALID_PARAMETER.getMsg());
        log.warn("ParamInvalid msg={}", msg);
        return ApiResponse.fail(BaseError.INVALID_PARAMETER.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Object> handleBindException(BindException ex) {
        String msg = ex.getAllErrors().stream().findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(BaseError.INVALID_PARAMETER.getMsg());
        log.warn("ParamBindFail msg={}", msg);
        return ApiResponse.fail(BaseError.INVALID_PARAMETER.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Object> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream().findFirst().map(ConstraintViolation::getMessage)
                .orElse(BaseError.INVALID_PARAMETER.getMsg());
        log.warn("ConstraintViolation msg={}", msg);
        return ApiResponse.fail(BaseError.INVALID_PARAMETER.getCode(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("BodyNotReadable msg={}", ex.getMessage());
        return ApiResponse.fail(BaseError.INVALID_PARAMETER);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceeded msg={}", ex.getMessage());
        return ApiResponse.fail(BaseError.FILE_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception ex) {
        if (ex instanceof RuntimeException runtimeEx) {
            log.error("RuntimeException msg={}", runtimeEx.getMessage(), runtimeEx);
            return ApiResponse.fail(BaseError.SERVICE_ERROR);
        }
        log.error("UnhandledException msg={}", ex.getMessage(), ex);
        return ApiResponse.fail(BaseError.INTERNAL_ERROR);
    }
}
