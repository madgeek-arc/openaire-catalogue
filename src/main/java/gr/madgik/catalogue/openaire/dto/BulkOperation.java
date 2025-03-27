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
