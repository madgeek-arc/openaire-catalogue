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

package gr.madgik.catalogue.openaire.config;

import gr.madgik.catalogue.openaire.domain.*;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private static final Logger logger = LoggerFactory.getLogger(CustomMethodSecurityExpressionRoot.class);


    private final RegistryCoreRepository<ProviderBundle, String> providerRepository;
    private final RegistryCoreRepository<ServiceBundle, String> serviceRepository;
    private final RegistryCoreRepository<DatasourceBundle, String> datasourceRepository;
    private final InvitationService invitationService;


    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CustomMethodSecurityExpressionRoot(Authentication authentication,
                                              RegistryCoreRepository<ProviderBundle, String> providerRepository,
                                              RegistryCoreRepository<ServiceBundle, String> serviceRepository,
                                              RegistryCoreRepository<DatasourceBundle, String> datasourceRepository,
                                              InvitationService invitationService) {
        super(authentication);
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.datasourceRepository = datasourceRepository;
        this.invitationService = invitationService;
    }


    /* ********************************************** */
    /*      MethodSecurityExpressionOperations        */
    /* ********************************************** */

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object o) {
        if (o != null) {
            if (o instanceof ResponseEntity<?>) {
                o = ((ResponseEntity<?>) o).getBody();
            }
        }
        this.returnObject = o;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }


    /* ********************************************** */
    /*       Custom Security Expression Methods       */
    /* ********************************************** */

    public boolean hasProviderInvitation(String invitationToken) {
        return invitationService.processInvitation(invitationToken, User.of(SecurityContextHolder.getContext().getAuthentication()).getEmail());
    }

    public <T extends Identifiable> boolean isProviderAdmin(T resource) {
        return isProviderAdmin(resource.getId());
    }

    public boolean isProviderAdmin(String providerId) {
        if (providerId == null) {
            return false;
        }
        User user = User.of(SecurityContextHolder.getContext().getAuthentication());
        ProviderBundle providerBundle = providerRepository.get(providerId);
        if (providerBundle.getProvider().getUsers() == null) {
            return false;
        }
        return providerBundle.getProvider().getUsers()
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(u -> u.getEmail().equals(user.getEmail()));
    }

    public <T extends Identifiable> boolean isServiceProviderAdmin(T resource) {
        return isServiceProviderAdmin(resource.getId());
    }

    public boolean isServiceProviderAdmin(String resourceId) {
        if (resourceId == null) {
            return false;
        }
        ServiceBundle serviceBundle = serviceRepository.get(resourceId);
        if (serviceBundle == null) {
            return false;
        }
        return isProviderAdmin(serviceBundle.getPayload().getResourceOrganisation());
    }

    public <T extends Identifiable> boolean isDatasourceProviderAdmin(T resource) {
        return isDatasourceProviderAdmin(resource.getId());
    }

    public boolean isDatasourceProviderAdmin(String resourceId) {
        if (resourceId == null) {
            return false;
        }
        DatasourceBundle datasourceBundle = datasourceRepository.get(resourceId);
        if (datasourceBundle == null) {
            return false;
        }
        return isServiceProviderAdmin(datasourceBundle.getDatasource().getServiceId());
    }

}
