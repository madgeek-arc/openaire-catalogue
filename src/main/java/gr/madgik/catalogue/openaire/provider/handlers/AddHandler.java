package gr.madgik.catalogue.openaire.provider.handlers;

import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.ActionHandler;
import gr.madgik.catalogue.Context;

public class AddHandler implements ActionHandler<ProviderBundle, String> {
    @Override
    public void preHandle(ProviderBundle providerBundle, Context ctx) {
        ActionHandler.super.preHandle(providerBundle, ctx);
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
