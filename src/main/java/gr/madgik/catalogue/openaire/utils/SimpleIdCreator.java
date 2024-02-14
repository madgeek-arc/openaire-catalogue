package gr.madgik.catalogue.openaire.utils;

import eu.einfracentral.domain.*;
import gr.madgik.catalogue.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SimpleIdCreator {

    SimpleIdCreator() {
    }

    public String createProviderId(Provider provider) {
        String providerId;
        if (provider.getAbbreviation() != null && !"".equals(provider.getAbbreviation()) &&
                !"null".equals(provider.getAbbreviation())) {
            providerId = provider.getAbbreviation();
        } else {
            throw new ValidationException("Provider must have an abbreviation.");
        }
        return sanitizeString(providerId);
    }

    public String createServiceId(Service service) {
        if (service.getResourceOrganisation() == null || service.getResourceOrganisation().equals("")) {
            throw new ValidationException("Resource must have a Resource Organisation.");
        }
        String serviceId;
        if (service.getAbbreviation() != null && !"".equals(service.getAbbreviation()) &&
                !"null".equals(service.getAbbreviation())) {
            serviceId = service.getAbbreviation();
        } else {
            throw new ValidationException("Resource must have an abbreviation.");
        }
        String provider = service.getResourceOrganisation();
        return String.format("%s.%s", provider, sanitizeString(serviceId));
    }

    public String sanitizeString(String input) {
        return StringUtils
                .stripAccents(input)
                .replaceAll("[\\n\\t\\s]+", " ")
                .replaceAll("\\s+$", "")
                .replaceAll("[^a-zA-Z0-9\\s\\-_/]+", "")
                .replaceAll("[/\\s]+", "_")
                .toLowerCase();
    }
}
