/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

public class PersistentIdentitySystem {

    /**
     * Specify the EntityType to which the persistent identifier is referring to.
     */
    @Schema
    private String persistentIdentityEntityType;

    /**
     * Specify the list of persistent identifier schemes used to refer to EntityTypes.
     */
    @Schema
    private List<String> persistentIdentityEntityTypeSchemes;

    public PersistentIdentitySystem() {
    }

    public PersistentIdentitySystem(String persistentIdentityEntityType, List<String> persistentIdentityEntityTypeSchemes) {
        this.persistentIdentityEntityType = persistentIdentityEntityType;
        this.persistentIdentityEntityTypeSchemes = persistentIdentityEntityTypeSchemes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistentIdentitySystem that = (PersistentIdentitySystem) o;
        return Objects.equals(persistentIdentityEntityType, that.persistentIdentityEntityType) && Objects.equals(persistentIdentityEntityTypeSchemes, that.persistentIdentityEntityTypeSchemes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(persistentIdentityEntityType, persistentIdentityEntityTypeSchemes);
    }

    @Override
    public String toString() {
        return "PersistentIdentitySystem{" +
                "persistentIdentityEntityType='" + persistentIdentityEntityType + '\'' +
                ", persistentIdentityEntityTypeSchemes=" + persistentIdentityEntityTypeSchemes +
                '}';
    }

    public String getPersistentIdentityEntityType() {
        return persistentIdentityEntityType;
    }

    public void setPersistentIdentityEntityType(String persistentIdentityEntityType) {
        this.persistentIdentityEntityType = persistentIdentityEntityType;
    }

    public List<String> getPersistentIdentityEntityTypeSchemes() {
        return persistentIdentityEntityTypeSchemes;
    }

    public void setPersistentIdentityEntityTypeSchemes(List<String> persistentIdentityEntityTypeSchemes) {
        this.persistentIdentityEntityTypeSchemes = persistentIdentityEntityTypeSchemes;
    }
}
