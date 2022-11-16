package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.provider.repository.ProviderMongoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ProviderCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCatalogueFactory.class);
    private final ProviderMongoRepository providerRepository;
    private final ProviderService providerService;
    private final JmsTemplate jmsTopicTemplate;
    private final ProviderMailService registrationMailService;

    public ProviderCatalogueFactory(ProviderMongoRepository providerRepository,
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
            public void preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration preHandle");
                providerService.onboard(providerBundle, null);
                providerBundle.setId(createId(providerBundle.getProvider()));
                addAuthenticatedUser(providerBundle.getProvider());
                providerService.validate(providerBundle);
//                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//                providerBundle.setMetadata(Metadata.createMetadata(User.of(auth).getFullName(), User.of(auth).getEmail()));
                sortFields(providerBundle);
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
            public void preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider update preHandle");
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
            public void preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider delete preHandle");
                providerRepository.delete(providerBundle);
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
        User authUser = User.of(SecurityContextHolder.getContext().getAuthentication());
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
