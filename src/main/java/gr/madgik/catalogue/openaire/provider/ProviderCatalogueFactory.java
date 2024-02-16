package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.*;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.Context;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.utils.SimpleIdCreator;
import gr.madgik.catalogue.openaire.utils.UserUtils;
import gr.madgik.catalogue.service.sync.ProviderSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderCatalogueFactory {

    private static final Logger logger = LoggerFactory.getLogger(ProviderCatalogueFactory.class);
    private final ProviderRepository providerRepository;
    private final ProviderService providerService;
    private final ProviderSync providerSync;
    private final ProviderResourcesCommonMethods commonMethods;
    private final SimpleIdCreator idCreator;

    public ProviderCatalogueFactory(ProviderRepository providerRepository,
                                    ProviderSync providerSync,
                                    @Lazy ProviderService providerService,
                                    ProviderResourcesCommonMethods commonMethods,
                                    SimpleIdCreator idCreator) {
        this.providerRepository = providerRepository;
        this.providerSync = providerSync;
        this.providerService = providerService;
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
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = UserUtils.getUserFromAuthentication(auth);
                commonMethods.onboard(providerBundle, auth);
                providerBundle.setId(idCreator.createProviderId(providerBundle.getProvider()));
                addAuthenticatedUser(providerBundle.getProvider(), user);
                providerBundle.setMetadata(Metadata.createMetadata(user.getFullName(), user.getEmail()));

                // validate
                providerService.validate(providerBundle);

                return providerBundle;
            }

            @Override
            public void postHandle(ProviderBundle providerBundle, Context ctx) {
                logger.info("Inside Provider registration postHandle");
//                serviceSync.syncAdd(providerBundle.getProvider());
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

                // validate
                commonMethods.prohibitCatalogueIdChange(providerBundle.getProvider().getCatalogueId());
                providerService.validate(providerBundle);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = User.of(auth);

                providerBundle.setMetadata(Metadata.updateMetadata(providerBundle.getMetadata(), user.getFullName(),
                        user.getEmail()));

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
//                serviceSync.syncUpdate(providerBundle.getProvider());
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
//                serviceSync.syncDelete(providerBundle.getProvider());
            }

            @Override
            public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
                logger.info("Inside Provider delete handleError");
            }
        });


        return catalogue;
    }

    private void addAuthenticatedUser(Provider provider, User authUser) {
        List<User> users = provider.getUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        if (users.stream().noneMatch(u -> u.getEmail().equalsIgnoreCase(authUser.getEmail()))) {
            users.add(authUser);
            provider.setUsers(users);
        }
    }
}
