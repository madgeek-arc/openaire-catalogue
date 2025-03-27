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

package gr.madgik.catalogue.openaire.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends RuntimeException {
    private HttpStatus status;

    public ValidationException(HttpStatus status) {
        this("ValidationException", status);
    }

    public ValidationException(String msg) {
        this(msg, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String msg, HttpStatus status) {
        super(msg);
        this.setStatus(status);
    }

    public ValidationException(Exception e, HttpStatus status) {
        super(e);
        this.setStatus(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    private void setStatus(HttpStatus status) {
        this.status = status;
    }
}
