package eu.openaire.catalogue.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a denied request with 401 when no user is signed in, and 403 otherwise.
 */
@RestControllerAdvice(basePackages = "eu.openaire.catalogue")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorHandler {

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail denied(AccessDeniedException e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        return signedIn
                ? ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage())
                : ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sign in is required");
    }
}
