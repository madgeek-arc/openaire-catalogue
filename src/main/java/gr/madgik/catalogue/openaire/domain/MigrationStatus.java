package gr.madgik.catalogue.openaire.domain;

import java.util.List;

public class MigrationStatus {

    private List<String> comments;

    private String modified;

    private String migrationDate;

    private String resolutionDate;

    private String modelVersion;

    public MigrationStatus() {
    }

    public MigrationStatus(List<String> comments, String modified, String migrationDate, String resolutionDate, String modelVersion) {
        this.comments = comments;
        this.modified = modified;
        this.migrationDate = migrationDate;
        this.resolutionDate = resolutionDate;
        this.modelVersion = modelVersion;
    }

    @Override
    public String toString() {
        return "MigrationStatus{" +
                "comments=" + comments +
                ", modified='" + modified + '\'' +
                ", migrationDate='" + migrationDate + '\'' +
                ", resolutionDate='" + resolutionDate + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getMigrationDate() {
        return migrationDate;
    }

    public void setMigrationDate(String migrationDate) {
        this.migrationDate = migrationDate;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
