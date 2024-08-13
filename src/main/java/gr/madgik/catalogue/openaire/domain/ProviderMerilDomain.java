package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class ProviderMerilDomain {


    // Provider's Location Information
    /**
     * MERIL scientific domain classification.
     */
    @Schema
    private String merilScientificDomain;

    /**
     * MERIL scientific subdomain classification.
     */
    @Schema
    private String merilScientificSubdomain;

    public ProviderMerilDomain() {
    }

    public ProviderMerilDomain(String merilScientificDomain, String merilScientificSubdomain) {
        this.merilScientificDomain = merilScientificDomain;
        this.merilScientificSubdomain = merilScientificSubdomain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderMerilDomain that = (ProviderMerilDomain) o;
        return Objects.equals(merilScientificDomain, that.merilScientificDomain) && Objects.equals(merilScientificSubdomain, that.merilScientificSubdomain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merilScientificDomain, merilScientificSubdomain);
    }

    @Override
    public String toString() {
        return "ProviderMerilDomain{" +
                "merilScientificDomain='" + merilScientificDomain + '\'' +
                ", merilScientificSubdomain='" + merilScientificSubdomain + '\'' +
                '}';
    }

    public String getMerilScientificDomain() {
        return merilScientificDomain;
    }

    public void setMerilScientificDomain(String merilScientificDomain) {
        this.merilScientificDomain = merilScientificDomain;
    }

    public String getMerilScientificSubdomain() {
        return merilScientificSubdomain;
    }

    public void setMerilScientificSubdomain(String merilScientificSubdomain) {
        this.merilScientificSubdomain = merilScientificSubdomain;
    }
}
