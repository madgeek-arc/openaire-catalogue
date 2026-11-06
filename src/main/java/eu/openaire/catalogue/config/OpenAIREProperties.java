package eu.openaire.catalogue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "openaire")
public class OpenAIREProperties {

    /**
     * Name used in the subject and body of notification emails.
     */
    private String projectName = "OpenAIRE";

    /**
     * Organisation assigned to services and datasources submitted without one.
     */
    private String defaultResourceOwner = "openaire";

    /**
     * Whether services and datasources stay active while their onboarding status is pending.
     */
    private boolean pendingResourcesActive = true;

    /**
     * Channels of a newly created service.
     */
    private List<String> defaultChannels = List.of("oa");

    private Invitations invitations = new Invitations();

    /**
     * Whether the OpenAIRE service and datasource models are stored at startup and applied to their resource types.
     */
    private boolean loadModels = true;

    public List<String> getDefaultChannels() {
        return defaultChannels;
    }

    public void setDefaultChannels(List<String> defaultChannels) {
        this.defaultChannels = defaultChannels;
    }

    public boolean isLoadModels() {
        return loadModels;
    }

    public void setLoadModels(boolean loadModels) {
        this.loadModels = loadModels;
    }

    public boolean isPendingResourcesActive() {
        return pendingResourcesActive;
    }

    public void setPendingResourcesActive(boolean pendingResourcesActive) {
        this.pendingResourcesActive = pendingResourcesActive;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDefaultResourceOwner() {
        return defaultResourceOwner;
    }

    public void setDefaultResourceOwner(String defaultResourceOwner) {
        this.defaultResourceOwner = defaultResourceOwner;
    }

    public Invitations getInvitations() {
        return invitations;
    }

    public void setInvitations(Invitations invitations) {
        this.invitations = invitations;
    }

    public static class Invitations {

        /**
         * Whether registering an organisation requires a valid invitation for users that are
         * neither administrators nor members of the onboarding team.
         */
        private boolean required = true;

        /**
         * Name of the request parameter carrying the invitation token.
         */
        private String parameter = "invitation";

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }
    }
}
