package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.domain.User;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
public class UserController {

    public UserController() {
    }

    @GetMapping("info")
    public ResponseEntity<User> getInfo(@Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(User.of(authentication), HttpStatus.OK);
    }
}

