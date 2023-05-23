package gr.madgik.catalogue.openaire.config;


import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    /** Use Lazy loading to avoid initialization issues.
     * (e.g. breaks advisors' configuration and @Transactional annotations do not work) **/
    @Lazy
    @Autowired
    private RegistryCoreRepository<ProviderBundle, String> providerRepository;

    @Lazy
    @Autowired
    private RegistryCoreRepository<ServiceBundle, String> serviceRepository;

    @Lazy
    @Autowired
    private RegistryCoreRepository<DatasourceBundle, String> datasourceRepository;

    @Lazy
    @Autowired
    private InvitationService invitationService;

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
        CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(authentication,
                providerRepository,
                serviceRepository,
                datasourceRepository,
                invitationService);
        root.setTrustResolver(this.trustResolver);
        root.setRoleHierarchy(getRoleHierarchy());
        return root;
    }
}
