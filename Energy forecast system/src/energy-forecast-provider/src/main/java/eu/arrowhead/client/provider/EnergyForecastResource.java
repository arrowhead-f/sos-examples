/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.can_be_modified.model.Entry;
import eu.arrowhead.client.common.can_be_modified.model.Message;
import eu.arrowhead.client.common.no_need_to_modify.ArrowheadResource;
import eu.arrowhead.client.common.no_need_to_modify.Orchestrator;
import eu.arrowhead.client.common.no_need_to_modify.Utility;
import eu.arrowhead.client.common.no_need_to_modify.misc.TypeSafeProperties;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRequestForm;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("forecast")
@Produces(MediaType.APPLICATION_JSON)
public class EnergyForecastResource extends ArrowheadResource {
    private final TypeSafeProperties props = Utility.getProp("app.properties");

    private Message get(UriBuilder url, long building, long from, long to) {
        final URI uri = url
                .queryParam("Building", building)
                .queryParam("Tstart", from)
                .queryParam("Tend", to)
                .build();
        return Utility.requestEntity("GET", uri.toString(), null, Message.class);
    }

    @GET
    public Response getIt(@Context SecurityContext context,
                          @QueryParam("token") String token,
                          @QueryParam("signature") String signature,
                          @QueryParam("building") long building,
                          @QueryParam("timestamp") long time
    ) {
        return verifiedResponse(context, token, signature, () -> {
            final boolean isSecure = context.isSecure();
            final Orchestrator orchestrator = new Orchestrator(isSecure);
            final ServiceRequestForm outdoorSrf = EnergyForecastProvider.buildServiceRequestForm("Outdoor", isSecure, props);
            final UriBuilder outdoorUrl = orchestrator.sendOrchestrationRequest(outdoorSrf);

            List<Entry> forecasts = get(outdoorUrl, building, time - 3600, time + 1800).getEntry();
            Entry forecast = forecasts.get(forecasts.size() - 1);

            Entry entry = new Entry();
            entry.setBuilding(building);
            Long timestamp = forecast.getTimestamp();
            entry.setTimestamp(timestamp);
            try {
                float total = Predicter.predictTotalUsage(building, forecast.getOutTemp());
                entry.setTotal(total);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                float water = Predicter.predictWaterUsage(building, new DateTime(time).getHourOfDay());
                entry.setWater(water);
            } catch (Exception e) {
                e.printStackTrace();
            }
            entry.setOutTemp(forecast.getOutTemp());
            ArrayList<Entry> entries = new ArrayList<>();
            entries.add(entry);
            Message response = new Message();
            response.setTstart(timestamp);
            response.setTend(timestamp);
            response.setEntries(entries);

            return response;
        });
    }
}
