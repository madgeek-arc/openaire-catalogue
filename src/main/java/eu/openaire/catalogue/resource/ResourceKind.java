package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;

import java.util.Arrays;

/**
 * The record types of the catalogue. The resource type name is also the key of the payload inside the bundle.
 */
public enum ResourceKind {

    PROVIDER("provider"),
    SERVICE("service"),
    DATASOURCE("datasource");

    private final String type;

    ResourceKind(String type) {
        this.type = type;
    }

    /**
     * The registry resource type, and the key of the payload inside the bundle.
     */
    public String type() {
        return type;
    }

    /**
     * @throws ResourceNotFoundException if no kind has the given resource type
     */
    public static ResourceKind of(String type) {
        return Arrays.stream(values())
                .filter(kind -> kind.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Unknown resource type '" + type + "'"));
    }
}
