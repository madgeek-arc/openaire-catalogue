package gr.madgik.catalogue.openaire.config;

import gr.athenarc.catalogue.controller.GenericExceptionController;
import gr.athenarc.catalogue.exception.ServerError;
import io.sentry.Sentry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ExceptionControllerAdvice extends GenericExceptionController {

    @ExceptionHandler({Throwable.class})
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        Sentry.captureException(ex);
        return super.handleException(req, ex);
    }

}
