package com.example.headlesscrm.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFoundException_ReturnsNotFoundProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Customer not found with id: 123");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Resource Not Found");
        assertThat(response.getBody().getDetail()).isEqualTo("Customer not found with id: 123");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleDuplicateResourceException_ReturnsConflictProblemDetail() {
        DuplicateResourceException ex = new DuplicateResourceException("Customer with email 'test@example.com' already exists");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDuplicateResourceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Duplicate Resource");
        assertThat(response.getBody().getDetail()).isEqualTo("Customer with email 'test@example.com' already exists");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleBadRequestException_ReturnsBadRequestProblemDetail() {
        BadRequestException ex = new BadRequestException("Invalid query parameter");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBadRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid query parameter");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleBadCredentialsException_ReturnsUnauthorizedProblemDetail() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBadCredentialsException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Authentication Failed");
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleAccessDeniedException_ReturnsForbiddenProblemDetail() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("You do not have permission to perform this action.");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Access Denied");
        assertThat(response.getBody().getDetail()).isEqualTo("You do not have permission to perform this action.");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void handleValidationExceptions_ReturnsValidationProblemDetailWithFieldErrors() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "customerRequest");
        bindingResult.addError(new FieldError("customerRequest", "email", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getProperties()).containsKey("invalidFields");

        @SuppressWarnings("unchecked")
        Map<String, String> invalidFields = (Map<String, String>) response.getBody().getProperties().get("invalidFields");
        assertThat(invalidFields).containsEntry("email", "must not be blank");
    }

    @Test
    void handleGlobalException_ReturnsInternalServerErrorProblemDetail() {
        Exception ex = new RuntimeException("Unexpected database failure");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleGlobalException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }
}
