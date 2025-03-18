package gr.madgik.catalogue.openaire.config;

import gr.uoa.di.madgik.catalogue.controller.GenericExceptionController;
import gr.uoa.di.madgik.catalogue.exception.ServerError;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionControllerAdvice extends GenericExceptionController {

    @ExceptionHandler({Throwable.class})
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        Sentry.captureException(ex);
        return super.handleException(req, ex);
    }

}
