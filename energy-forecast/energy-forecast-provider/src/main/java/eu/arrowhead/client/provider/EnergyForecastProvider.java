/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.common.Message;
import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.api.clients.RestClient;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.ServiceRegistryEntry;
import org.joda.time.DateTime;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EnergyForecastProvider extends ArrowheadApplication {

    private RestClient outdoorClient;
    private RestClient indoorClient;

    public static void main(String[] args) {
        new EnergyForecastProvider(args).start(true);
    }

    private EnergyForecastProvider(String[] args) {
        super(args);
    }

    @Override
    protected void onStart() {
        final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);

        final ArrowheadServer server = ArrowheadServer
                .createFromProperties(securityContext)
                .addResources(EnergyForecastResource.class)
                .start();

        ServiceRegistryClient
                .createFromProperties(securityContext)
                .register(ServiceRegistryEntry.createFromProperties(server));

        final OrchestrationClient orchestrationClient = OrchestrationClient.createFromProperties(securityContext);
        final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);

        outdoorClient = EnergyForecastUtils.createClient(orchestrationClient, me, "Outdoor");
        indoorClient = EnergyForecastUtils.createClient(orchestrationClient, me, "Indoor");

        updateData();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateData, 7, 7, TimeUnit.DAYS);
    }

    @Override
    protected void onStop() {

    }

    private void updateData() {
        log.info("Updating data and learning model");
        try {
            final long now = DateTime.now().getMillis() / 1000;
            final Message outdoorData = outdoorClient.get()
                    .queryParam("Tstart", Predicter.lastConsumptionTimeStamp())
                    .queryParam("Tend", now)
                    .send().readEntity(Message.class);
            Predicter.update(outdoorData.getEntry());
            final Message indoorData = indoorClient.get()
                    .queryParam("Tstart", Predicter.lastIndoorTimeStamp())
                    .queryParam("Tend", now)
                    .send().readEntity(Message.class);
            Predicter.update(indoorData.getEntry());
        } catch (Exception e) {
            log.error("Error while learning", e);
        }
    }

}
