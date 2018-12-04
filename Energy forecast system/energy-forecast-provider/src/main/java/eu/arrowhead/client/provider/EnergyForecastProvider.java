/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.can_be_modified.model.Message;
import eu.arrowhead.client.common.no_need_to_modify.ArrowheadProps;
import eu.arrowhead.client.common.no_need_to_modify.ArrowheadProvider;
import eu.arrowhead.client.common.no_need_to_modify.Orchestrator;
import eu.arrowhead.client.common.no_need_to_modify.Utility;
import eu.arrowhead.client.common.no_need_to_modify.exception.ArrowheadException;
import eu.arrowhead.client.common.no_need_to_modify.model.*;
import org.joda.time.DateTime;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EnergyForecastProvider extends ArrowheadProvider {

    private final UriBuilder indoorUrl;
    private final UriBuilder outdoorUrl;

    private EnergyForecastProvider(String[] args) {
        super(args,
                new Class[] {EnergyForecastResource.class},
                new String[] {"eu.arrowhead.client.common"});

        ServiceRegistryEntry srEntry = ArrowheadProps.getServiceRegistryEntry(props, baseUri, isSecure, base64PublicKey);
        System.out.println("Service Registry Entry: " + Utility.toPrettyJson(null, srEntry));
        registerToServiceRegistry(srEntry);

        final ServiceRequestForm indoorSrf = buildServiceRequestForm("Indoor", isSecure, props);
        final ServiceRequestForm outdoorSrf = buildServiceRequestForm("Outdoor", isSecure, props);

        final Orchestrator orchestrator = new Orchestrator(isSecure);
        indoorUrl = orchestrator.sendOrchestrationRequest(indoorSrf);
        outdoorUrl = orchestrator.sendOrchestrationRequest(outdoorSrf);

        updateData();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateData, 7, 7, TimeUnit.DAYS);

        listenForInput();
    }

    private void updateData() {
        System.out.println("Updating data and learning model");
        try {
            final long now = DateTime.now().getMillis() / 1000;
            final Message outdoorData = get(outdoorUrl, Predicter.lastConsumptionTimeStamp(), now);
            Predicter.update(outdoorData.getEntry());
            long from = Predicter.lastIndoorTimeStamp();
            final Message indoorData = get(indoorUrl, from, now);
            Predicter.update(indoorData.getEntry());
        } catch (Exception e) {
            System.out.println("Error while learning");
            e.printStackTrace();
        }
    }

    private Message get(UriBuilder url, long from, long to) {
        final URI uri = url.clone()
                .queryParam("Tstart", from)
                .queryParam("Tend", to)
                .build();
        return Utility.requestEntity("GET", uri.toString(), null, Message.class);
    }

    static ServiceRequestForm buildServiceRequestForm(String serviceDefinition, boolean isSecure, Properties props) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("unit", "celsius");
        if (isSecure) {
            metadata.put("security", "token");
        }

        ArrowheadService service = new ArrowheadService(serviceDefinition, Collections.singletonList("json"), metadata);

        Map<String, Boolean> orchestrationFlags = new HashMap<>();
        orchestrationFlags.put("overrideStore", true);
        orchestrationFlags.put("pingProviders", false);
        orchestrationFlags.put("metadataSearch", true);
        orchestrationFlags.put("enableInterCloud", true);

        ArrowheadSystem consumer;
        if (isSecure) {
            String secProviderName = props.getProperty("secure_system_name");
            consumer = new ArrowheadSystem(secProviderName, "null", 0, "null");
        } else {
            String insecProviderName = props.getProperty("insecure_system_name");
            consumer = new ArrowheadSystem(insecProviderName, "null", 0, "null");
        }

        return new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
    }

    public static void main(String[] args) {
        new EnergyForecastProvider(args);
    }

}
