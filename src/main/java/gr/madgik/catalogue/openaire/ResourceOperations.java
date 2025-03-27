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

package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.dto.BulkOperation;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceOperations<T, ID> {

    List<T> get(ID... ids);

    boolean validate(T t);

    T create(T resource);

    T update(ID id, T resource);

    void delete(ID id);

    T get(ID id);

    Paging<T> get(FacetFilter filter);

    default Page<T> get(Pageable filter) {
        throw new UnsupportedOperationException();
    }

    BulkOperation<T> bulkAdd(List<T> resources);

    BulkOperation<T> bulkUpdate(List<T> resources);
}
