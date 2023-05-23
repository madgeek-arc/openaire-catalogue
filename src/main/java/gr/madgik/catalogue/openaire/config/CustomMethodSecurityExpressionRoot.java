package gr.madgik.catalogue.openaire.config;

import eu.einfracentral.domain.Identifiable;
import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

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
        return invitationService.accept(invitationToken, User.of(this.authentication).getEmail());
    }

    public <T extends Identifiable> boolean isProviderAdmin(T resource) {
        return isProviderAdmin(resource.getId());
    }

    public boolean isProviderAdmin(String providerId) {
        if (providerId == null) {
            return false;
        }
        User user = User.of(this.authentication);
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
        return isProviderAdmin(datasourceBundle.getPayload().getResourceOrganisation());
    }

}
