package com.trever.backend.common.advice;

import com.trever.backend.common.exception.BaseException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestControllerAdvice
public class ControllerExceptionAdvice {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse> handleGlobalException(BaseException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.fail(ex.getStatusCode(), ex.getResponseMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(),
                        ErrorStatus.VALIDATION_REQUEST_MISSING_EXCEPTION.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse>  handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        FieldError fieldError = Objects.requireNonNull(e.getFieldError());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(),String.format("%s. (%s)", fieldError.getDefaultMessage(), fieldError.getField())));
    }

    /**
     * 비동기 처리 예외
     */
    @ExceptionHandler({CompletionException.class, ExecutionException.class})
    public ResponseEntity<ApiResponse<?>> handleAsyncException(Exception ex) {
        // 원인 예외 추출
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }


        // 400 Bad Request 상태 코드로 응답
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .status(400)
                        .message(cause.getMessage())
                        .build());
    }



}