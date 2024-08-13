package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URL;
import java.util.Objects;

public class ResearchProductLicensing {

    /**
     * Research product license name
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String researchProductLicenseName;

    /**
     * Research product license URL
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private URL researchProductLicenseURL;

    public ResearchProductLicensing() {
    }

    public ResearchProductLicensing(String researchProductLicenseName, URL researchProductLicenseURL) {
        this.researchProductLicenseName = researchProductLicenseName;
        this.researchProductLicenseURL = researchProductLicenseURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchProductLicensing that = (ResearchProductLicensing) o;
        return Objects.equals(researchProductLicenseName, that.researchProductLicenseName) && Objects.equals(researchProductLicenseURL, that.researchProductLicenseURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(researchProductLicenseName, researchProductLicenseURL);
    }

    @Override
    public String toString() {
        return "ResearchProductLicensing{" +
                "researchProductLicenseName='" + researchProductLicenseName + '\'' +
                ", researchProductLicenseURL=" + researchProductLicenseURL +
                '}';
    }

    public String getResearchProductLicenseName() {
        return researchProductLicenseName;
    }

    public void setResearchProductLicenseName(String researchProductLicenseName) {
        this.researchProductLicenseName = researchProductLicenseName;
    }

    public URL getResearchProductLicenseURL() {
        return researchProductLicenseURL;
    }

    public void setResearchProductLicenseURL(URL researchProductLicenseURL) {
        this.researchProductLicenseURL = researchProductLicenseURL;
    }
}
