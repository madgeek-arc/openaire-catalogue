package gr.madgik.catalogue.openaire.utils;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.LoggingInfo;
import gr.madgik.catalogue.SecurityService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderResourcesCommonMethods {

    private static final Logger logger = LogManager.getLogger(ProviderResourcesCommonMethods.class);

    private final SecurityService securityService;

    public ProviderResourcesCommonMethods(SecurityService securityService) {
        this.securityService = securityService;
    }

    public List<LoggingInfo> returnLoggingInfoListAndCreateRegistrationInfoIfEmpty(Bundle<?> bundle, Authentication auth) {
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
        if (bundle.getLoggingInfo() != null && !bundle.getLoggingInfo().isEmpty()) {
            loggingInfoList = bundle.getLoggingInfo();
        } else {
            loggingInfoList.add(createLoggingInfo(auth, LoggingInfo.Types.ONBOARD.getKey(),
                    LoggingInfo.ActionType.REGISTERED.getKey()));
        }
        return loggingInfoList;
    }

    public LoggingInfo createLoggingInfo(Authentication auth, String type, String actionType) {
        return LoggingInfo.createLoggingInfoEntry(auth, securityService.getRoleName(auth), type, actionType);
    }
}
