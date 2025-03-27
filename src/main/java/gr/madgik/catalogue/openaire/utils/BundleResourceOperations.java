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

package gr.madgik.catalogue.openaire.utils;

import gr.madgik.catalogue.openaire.domain.Bundle;
import gr.madgik.catalogue.openaire.domain.Identifiable;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;

import java.util.List;

public interface BundleResourceOperations<T extends Identifiable, B extends Bundle<T>, ID> {

    boolean validate(Object resource);

    T register(T resource);

    B create(B resource);

    B update(ID id, B resource);

    void delete(ID id);

    T get(ID id);

    B getBundle(ID id);

    Paging<B> getWithEnrichedFacets(FacetFilter filter);

    Paging<B> get(FacetFilter filter);

    List<T> getByIds(ID... ids);

    B verify(ID id, String status, Boolean active);

    B activate(ID id, Boolean active);
}
