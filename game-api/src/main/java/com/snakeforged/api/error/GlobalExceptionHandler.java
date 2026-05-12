package com.snakeforged.api.error;

import com.snakeforged.domain.DifficultyResolutionException;
import com.snakeforged.domain.ImplausibleScoreException;
import com.snakeforged.observability.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice(basePackages = "com.snakeforged")
public class GlobalExceptionHandler {

    private static final String HIGHSCORE_PATH = "/api/v1/highscores";

    private final MetricsService metricsService;
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MetricsService metricsService, MessageSource messageSource) {
        this.metricsService = metricsService;
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req,
                                                          Locale locale) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String message = fieldErrors.isEmpty()
                ? messageSource.getMessage("error.validation.summary", null, locale)
                : fieldErrors.get(0).message();
        if (HIGHSCORE_PATH.equals(req.getRequestURI())) {
            metricsService.recordRejection();
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message, req.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(DifficultyResolutionException.class)
    public ResponseEntity<ErrorResponse> handleDifficultyUnknown(DifficultyResolutionException ex,
                                                                 HttpServletRequest req,
                                                                 Locale locale) {
        String message = messageSource.getMessage(
                "error.difficulty.unknown",
                new Object[]{ex.getRequestedName()},
                locale);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(ImplausibleScoreException.class)
    public ResponseEntity<ErrorResponse> handleImplausibleScore(ImplausibleScoreException ex,
                                                               HttpServletRequest req,
                                                               Locale locale) {
        String message = messageSource.getMessage(
                "error.score.implausible",
                new Object[]{ex.getScore(), ex.getDifficulty()},
                locale);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req,
                                                            Locale locale) {
        String message = messageSource.getMessage(
                "error.param.missing",
                new Object[]{ex.getParameterName()},
                locale);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest req,
                                                          Locale locale) {
        String message = messageSource.getMessage("error.body.malformed", null, locale);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex,
                                                        HttpServletRequest req,
                                                        Locale locale) {
        String message = messageSource.getMessage(
                "error.endpoint.not_found",
                new Object[]{req.getRequestURI()},
                locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req, Locale locale) {
        String message = messageSource.getMessage("error.internal", null, locale);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, req.getRequestURI()));
    }
}
