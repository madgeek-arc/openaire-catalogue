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

package gr.madgik.catalogue.openaire.provider.controller;

import gr.madgik.catalogue.openaire.domain.Provider;
import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.domain.User;
import gr.madgik.catalogue.openaire.invitations.Invitation;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/providers")
public class ProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderService providerService;
    private final InvitationService invitationService;

    public ProviderController(ProviderService providerService, InvitationService invitationService) {
        this.providerService = providerService;
        this.invitationService = invitationService;
    }

    @GetMapping("{id}")
    public Provider get(@PathVariable("id") String id) {
        return providerService.get(id);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or hasProviderInvitation(#invitationToken)")
    public Provider add(@RequestBody Provider provider, @RequestParam(name = "invitation", required = false) String invitationToken) {
        Invitation invitation = invitationService.validateAndConstructInvitation(invitationToken);
        invitationService.accept(invitation);
        return providerService.register(provider);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isProviderAdmin(#provider)")
    public Provider update(@PathVariable String id, @RequestBody Provider provider) {
        return providerService.update(id, new ProviderBundle(provider)).getProvider(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        providerService.delete(id);
    }

    @GetMapping("my")
    public List<Provider> getMy(@Parameter(hidden = true) Authentication authentication) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("users", User.of(authentication).getEmail());
        return providerService.get(filter).map(ProviderBundle::getPayload).getResults();
    }

    @BrowseParameters
    @GetMapping
    public Paging<Provider> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        return providerService.getWithEnrichedFacets(FacetFilter.from(allRequestParams)).map(ProviderBundle::getPayload);
    }

    @PostMapping("validate")
    public boolean validate(@RequestBody Provider provider) {
        logger.info("Validating Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return providerService.validate(provider);
    }
}
