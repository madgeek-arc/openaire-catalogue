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

public class ResourceExtras {

    @Schema
    private List<EOSCIFGuidelines> eoscIFGuidelines;

    public ResourceExtras() {
    }

    public ResourceExtras(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }

    @Override
    public String toString() {
        return "ResourceExtras{" +
                "eoscIFGuidelines=" + eoscIFGuidelines +
                '}';
    }

    public List<EOSCIFGuidelines> getEoscIFGuidelines() {
        return eoscIFGuidelines;
    }

    public void setEoscIFGuidelines(List<EOSCIFGuidelines> eoscIFGuidelines) {
        this.eoscIFGuidelines = eoscIFGuidelines;
    }
}
