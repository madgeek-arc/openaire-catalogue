package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class ServiceProviderDomain {


    // Provider's Location Information
    /**
     * The branch of science, scientific discipline that is related to the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String scientificDomain;

    /**
     * The subbranch of science, scientific sub-discipline that is related to the Resource.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String scientificSubdomain;

    public ServiceProviderDomain() {
    }

    public ServiceProviderDomain(String scientificDomain, String scientificSubdomain) {
        this.scientificDomain = scientificDomain;
        this.scientificSubdomain = scientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceProviderDomain that = (ServiceProviderDomain) o;
        return Objects.equals(scientificDomain, that.scientificDomain) && Objects.equals(scientificSubdomain, that.scientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scientificDomain, scientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderDomains{" +
                "scientificDomain='" + scientificDomain + '\'' +
                ", scientificSubdomain='" + scientificSubdomain + '\'' +
                '}';
    }

    public String getScientificDomain() {
        return scientificDomain;
    }

    public void setScientificDomain(String scientificDomain) {
        this.scientificDomain = scientificDomain;
    }

    public String getScientificSubdomain() {
        return scientificSubdomain;
    }

    public void setScientificSubdomain(String scientificSubdomain) {
        this.scientificSubdomain = scientificSubdomain;
    }
}
