package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URL;
import java.util.Objects;

public class ResearchProductMetadataLicensing {

    /**
     * Research Product Metadata License Name
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String researchProductMetadataLicenseName;

    /**
     * Research Product Metadata License URL
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private URL researchProductMetadataLicenseURL;

    public ResearchProductMetadataLicensing() {
    }

    public ResearchProductMetadataLicensing(String researchProductMetadataLicenseName, URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductMetadataLicensing that = (ResearchProductMetadataLicensing) o;
        return Objects.equals(researchProductMetadataLicenseName, that.researchProductMetadataLicenseName) && Objects.equals(researchProductMetadataLicenseURL, that.researchProductMetadataLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductMetadataLicenseName, researchProductMetadataLicenseURL);
    }

    @Override
    public String toString() {
        return "ResearchProductMetadataLicensing{" +
                "researchProductMetadataLicenseName='" + researchProductMetadataLicenseName + '\'' +
                ", researchProductMetadataLicenseURL=" + researchProductMetadataLicenseURL +
                '}';
    }

    public String getResearchProductMetadataLicenseName() {
        return researchProductMetadataLicenseName;
    }

    public void setResearchProductMetadataLicenseName(String researchProductMetadataLicenseName) {
        this.researchProductMetadataLicenseName = researchProductMetadataLicenseName;
    }

    public URL getResearchProductMetadataLicenseURL() {
        return researchProductMetadataLicenseURL;
    }

    public void setResearchProductMetadataLicenseURL(URL researchProductMetadataLicenseURL) {
        this.researchProductMetadataLicenseURL = researchProductMetadataLicenseURL;
    }
}
