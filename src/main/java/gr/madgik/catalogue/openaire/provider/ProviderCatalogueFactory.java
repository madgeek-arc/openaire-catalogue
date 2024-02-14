package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.*;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.utils.SimpleIdCreator;
import gr.madgik.catalogue.openaire.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProviderCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCatalogueFactory.class);
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;
    private final JmsTemplate jmsTopicTemplate;
    private final ProviderMailService registrationMailService;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SimpleIdCreator idCreator;

    public ProviderCatalogueFactory(ProviderRepository providerRepository,
                                    @Lazy ProviderService providerService,
                                    JmsTemplate jmsTopicTemplate,
                                    ProviderMailService registrationMailService,
                                    ProviderResourcesCommonMethods commonMethods,
                                    SimpleIdCreator idCreator) {
        this.providerRepository = providerRepository;
        this.providerService = providerService;
        this.jmsTopicTemplate = jmsTopicTemplate;
        this.registrationMailService = registrationMailService;
        this.commonMethods = commonMethods;
        this.idCreator = idCreator;
    }

    @Bean
    public Catalogue<ProviderBundle, String> getProviderCatalogue() {
        Catalogue<ProviderBundle, String> catalogue = new Catalogue<>(providerRepository);

        catalogue.registerHandler(Catalogue.Action.REGISTER, new ActionHandler<>() {
            @Override
            public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration preHandle");
                providerService.onboard(providerBundle, null);
                providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));
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
                loggingInfo.setDate(String.valueOf(System.currentTimeMillis()));
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

                List<LoggingInfo> loggingInfoList = commonMethods.returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(providerBundle, auth);
                LoggingInfo loggingInfo = commonMethods.createLoggingInfo(auth, LoggingInfo.Types.UPDATE.getKey(),
                        LoggingInfo.ActionType.UPDATED.getKey());
                loggingInfoList.add(loggingInfo);
                providerBundle.setLoggingInfo(loggingInfoList);
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
