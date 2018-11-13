/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer;
import eu.arrowhead.common.api.clients.core.ServiceRegistryClient;
import eu.arrowhead.common.exception.NotFoundException;
import eu.arrowhead.common.model.ServiceRegistryEntry;

public class OutdoorProvider extends ArrowheadApplication {

    public static void main(String[] args) {
        new OutdoorProvider(args).start();
    }

    public OutdoorProvider(String[] args) {
        super(args);
    }

    @Override
    protected void onStart() throws NotFoundException {
        final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
        final ArrowheadHttpServer server = ArrowheadGrizzlyHttpServer
                .createFromProperties(securityContext)
                .addResources(OutdoorResource.class)
                .start();

        ServiceRegistryClient
                .createFromProperties(securityContext)
                .register(ServiceRegistryEntry.createFromProperties(server));
    }

    @Override
    protected void onStop() {

    }

}
