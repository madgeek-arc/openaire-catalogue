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

package gr.madgik.catalogue.openaire.provider.handlers;

import gr.madgik.catalogue.openaire.ActionHandler;
import gr.madgik.catalogue.openaire.Context;
import gr.madgik.catalogue.openaire.domain.ProviderBundle;

public class AddHandler implements ActionHandler<ProviderBundle, String> {
    @Override
    public ProviderBundle preHandle(ProviderBundle providerBundle, Context ctx) {
        return ActionHandler.super.preHandle(providerBundle, ctx);
    }

    @Override
    public void postHandle(ProviderBundle providerBundle, Context ctx) {
        ActionHandler.super.postHandle(providerBundle, ctx);
    }

    @Override
    public void handleError(ProviderBundle providerBundle, Throwable throwable, Context ctx) {
        ActionHandler.super.handleError(providerBundle, throwable, ctx);
    }
}
