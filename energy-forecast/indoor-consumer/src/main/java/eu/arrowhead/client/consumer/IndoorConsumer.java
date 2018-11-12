/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.consumer;

import eu.arrowhead.common.Message;
import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.api.clients.RestClient;
import eu.arrowhead.common.misc.Utility;
import eu.arrowhead.common.model.*;

public class IndoorConsumer extends ArrowheadApplication {
    public static void main(String[] args) {
        new IndoorConsumer(args).start(false);
    }

    private IndoorConsumer(String[] args) {
        super(args);
    }

    @Override
    protected void onStart() {
        final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
        final ArrowheadSystem me = ArrowheadSystem.createFromProperties();
        final OrchestrationClient orchestrationClient = OrchestrationClient.createFromProperties(securityContext);
        final ServiceRequestForm serviceRequestForm = new ServiceRequestForm.Builder(me)
                .requestedService("indoor", "json", getProps().isSecure())
                .flag(OrchestrationFlags.Flags.OVERRIDE_STORE, true)
                .flag(OrchestrationFlags.Flags.PING_PROVIDERS, false)
                .flag(OrchestrationFlags.Flags.METADATA_SEARCH, true)
                .flag(OrchestrationFlags.Flags.ENABLE_INTER_CLOUD, false)
                .build();
        final RestClient indoorClient = orchestrationClient.buildClient(serviceRequestForm);
        final Message message = indoorClient.get().send().readEntity(Message.class);
        System.out.println("Provider Response payload: " + Utility.toPrettyJson(null, message));
    }

    @Override
    protected void onStop() {

    }

}
