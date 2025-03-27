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

package gr.madgik.catalogue.openaire.resource.repository;

import gr.madgik.catalogue.openaire.domain.Bundle;
import gr.madgik.catalogue.openaire.domain.Identifiable;
import gr.madgik.catalogue.openaire.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ResourceRepository<B extends Bundle<? extends Identifiable>, ID> extends Repository<B, ID> {

    B get(ID id, String catalogue);
}
