/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.invitations;

import gr.madgik.catalogue.openaire.domain.User;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "invitations", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;

    public InvitationController(InvitationRepository invitationRepository, InvitationService invitationService) {
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Invitation> getAll() {
        return invitationRepository.findAll();
    }

    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public String create(@RequestParam String email, @Parameter(hidden = true) Authentication authentication) {
        return invitationService.create(User.of(authentication), email);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        invitationRepository.deleteById(UUID.fromString(id));
    }

}
