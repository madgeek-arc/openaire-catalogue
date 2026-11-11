package eu.openaire.catalogue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Who reviews registrations.
 */
@ConfigurationProperties(prefix = "catalogue")
public class CatalogueProperties {

    /**
     * Emails of the administrators.
     */
    private List<String> admins = new ArrayList<>();

    /**
     * Emails of the onboarding team, who review registrations alongside the administrators.
     */
    private List<String> onboardingTeam = new ArrayList<>();

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public List<String> getOnboardingTeam() {
        return onboardingTeam;
    }

    public void setOnboardingTeam(List<String> onboardingTeam) {
        this.onboardingTeam = onboardingTeam;
    }
}
