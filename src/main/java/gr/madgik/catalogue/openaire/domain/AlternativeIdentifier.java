package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class AlternativeIdentifier {

    @Schema
    private String type;

    @Schema
    private String value;

    public AlternativeIdentifier() {
    }

    public AlternativeIdentifier(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "AlternativeIdentifier{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlternativeIdentifier that = (AlternativeIdentifier) o;
        return Objects.equals(type, that.type) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
