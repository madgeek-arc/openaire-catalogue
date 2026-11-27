package eu.openaire.catalogue.api;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The retired {@code /catalogue-resources} routes. The listing redirects to the service listing with the same query;
 * it no longer includes datasources. Its sub-routes answer 404.
 */
@Hidden
@RestController
public class CatalogueResourcesController {

    @GetMapping("catalogue-resources")
    public ResponseEntity<Void> list(HttpServletRequest request) {
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/oa/services")
                .query(request.getQueryString())
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    @GetMapping("catalogue-resources/**")
    public ResponseEntity<ProblemDetail> subRoute() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "/catalogue-resources has been removed; use /oa/services and /oa/datasources"));
    }
}
