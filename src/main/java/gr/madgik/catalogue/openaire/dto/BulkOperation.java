package gr.madgik.catalogue.openaire.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkOperation<T> {

    List<T> successful;
    List<T> failed;

    public BulkOperation() {
        this.successful = new ArrayList<>();
        this.failed = new ArrayList<>();
    }

    public BulkOperation(List<T> successful, List<T> failed) {
        this.successful = successful;
        this.failed = failed;
    }

    public List<T> getSuccessful() {
        return successful;
    }

    public void setSuccessful(List<T> successful) {
        this.successful = successful;
    }

    public List<T> getFailed() {
        return failed;
    }

    public void setFailed(List<T> failed) {
        this.failed = failed;
    }
}
