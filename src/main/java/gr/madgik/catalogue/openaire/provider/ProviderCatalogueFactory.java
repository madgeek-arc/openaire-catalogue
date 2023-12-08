package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.*;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.utils.UserUtils;
import org.apache.commons.lang3.StringUtils;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCatalogueFactory.class);
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;
    private final JmsTemplate jmsTopicTemplate;
    private final ProviderMailService registrationMailService;

    public ProviderCatalogueFactory(ProviderRepository providerRepository,
                                    @Lazy ProviderService providerService,
                                    JmsTemplate jmsTopicTemplate,
                                    ProviderMailService registrationMailService) {
        this.providerRepository = providerRepository;
        this.providerService = providerService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.registrationMailService = registrationMailService;
    }

    @Bean
    public Catalogue<ProviderBundle, String> getProviderCatalogue() {
        Catalogue<ProviderBundle, String> catalogue = new Catalogue<>(providerRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration preHandle");
                providerService.onboard(providerBundle, null);
                providerBundle.setId(createId(providerBundle.getProvider()));
                addAuthenticatedUser(providerBundle.getProvider());
                providerService.validate(providerBundle);
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = UserUtils.getUserFromAuthentication(auth);

                providerBundle.setMetadata(Metadata.createMetadata(user.getFullName(), user.getEmail()));

                LoggingInfo loggingInfo = new LoggingInfo();
                loggingInfo.setActionType(LoggingInfo.ActionType.REGISTERED.getKey());
                loggingInfo.setType(LoggingInfo.Types.ONBOARD.getKey());
                loggingInfo.setUserEmail(user.getEmail());
                loggingInfo.setUserFullName(user.getFullName());
                loggingInfo.setDate(new Date().toString());
                loggingInfo.setUserRole("");

                providerBundle.setLoggingInfo(new LinkedList<>());
                providerBundle.getLoggingInfo().add(loggingInfo);
                providerBundle.setLatestUpdateInfo(loggingInfo);
                sortFields(providerBundle);

                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration postHandle");

                registrationMailService.sendEmailsToNewlyAddedAdmins(providerBundle, null);

                jmsTopicTemplate.convertAndSend("provider.create", providerBundle.getProvider());

//                synchronizerServiceProvider.syncAdd(provider.getProvider());
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider registration handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.UPDATE, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider update preHandle");

                ProviderBundle existing = providerRepository.get(providerBundle.getId());
                existing.setProvider(providerBundle.getProvider());
                providerBundle = existing;

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = User.of(auth);

                providerBundle.setMetadata(Metadata.createMetadata(user.getFullName(), user.getEmail()));

                LoggingInfo loggingInfo = new LoggingInfo();
                loggingInfo.setActionType(LoggingInfo.ActionType.UPDATED.getKey());
                loggingInfo.setType(LoggingInfo.Types.UPDATE.getKey());
                loggingInfo.setUserEmail(user.getEmail());
                loggingInfo.setUserFullName(user.getFullName());
                loggingInfo.setDate(new Date().toString());
                loggingInfo.setUserRole("");

                if (providerBundle.getLoggingInfo() == null) {
                    providerBundle.setLoggingInfo(new ArrayList<>());
                }
                providerBundle.getLoggingInfo().add(loggingInfo);
                providerBundle.setLatestUpdateInfo(loggingInfo);

                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider update postHandle");
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider update handleError");
            }
        });

        catalogue.registerHandler(Catalogue.Action.DELETE, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider delete preHandle");
                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider delete postHandle");
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider delete handleError");
            }
        });


        return catalogue;
    }

    // FIXME: remove this method ?
    private String createId(Provider provider) {
        String providerId;
        if (provider.getId() == null || "".equals(provider.getId())) {
            if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation()) && !"null".equals(provider.getAbbreviation())) {
                providerId = provider.getAbbreviation();
            } else if (provider.getName() != null && !"".equals(provider.getName()) && !"null".equals(provider.getName())) {
                providerId = provider.getName();
            } else {
//                throw new ValidationException("Provider must have an acronym or name.");
                throw new RuntimeException("Provider must have an acronym or name.");
            }
        } else {
            providerId = provider.getId();
        }
        return StringUtils
                .stripAccents(providerId)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase();

    }

    private void addAuthenticatedUser(Provider provider) {
        List<User> users;
        User authUser = UserUtils.getUserFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
    }

    private void sortFields(ProviderBundle provider) {
        if (provider.getProvider().getParticipatingCountries() != null && !provider.getProvider().getParticipatingCountries().isEmpty()) {
            provider.getProvider().setParticipatingCountries(sortCountries(provider.getProvider().getParticipatingCountries()));
        }
    }

    public List<String> sortCountries(List<String> countries) {
        Collections.sort(countries);
        return countries;
    }
}
