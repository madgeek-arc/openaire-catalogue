package gr.madgik.catalogue.openaire.domain;

public class Identifiers {

    private String originalId;

    public Identifiers() {
    }

    public Identifiers(Identifiers identifiers) {
        this.originalId = identifiers.getOriginalId();
    }

    @Override
    public String toString() {
        return "Identifiers{" +
                "originalId='" + originalId + '\'' +
                '}';
    }

    public static void createOriginalId(Bundle<?> bundle) {
        if (bundle.getIdentifiers() != null) {
            bundle.getIdentifiers().setOriginalId(bundle.getId());
        } else {
            Identifiers identifiers = new Identifiers();
            identifiers.setOriginalId(bundle.getId());
            bundle.setIdentifiers(identifiers);
        }
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
