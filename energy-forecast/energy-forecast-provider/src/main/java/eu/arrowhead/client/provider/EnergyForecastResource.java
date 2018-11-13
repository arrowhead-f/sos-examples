/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.common.Entry;
import eu.arrowhead.common.Message;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.OrchestrationClient;
import eu.arrowhead.common.api.clients.RestClient;
import eu.arrowhead.common.api.resources.ArrowheadResource;
import eu.arrowhead.common.model.ArrowheadSystem;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;

@Path("forecast")
@Produces(MediaType.APPLICATION_JSON)
public class EnergyForecastResource extends ArrowheadResource {
    private final RestClient outdoorClient;

    @Context
    private Configuration configuration;

    public EnergyForecastResource(ArrowheadHttpServer server) {
        super(server);

        final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties();
        final OrchestrationClient orchestrationClient = OrchestrationClient.createFromProperties(securityContext);
        final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);
        outdoorClient = EnergyForecastUtils.createClient(orchestrationClient, me, "Outdoor");
    }

    @GET
    public Response getIt(@Context SecurityContext context,
                          @QueryParam("token") String token,
                          @QueryParam("signature") String signature,
                          @QueryParam("building") long building,
                          @QueryParam("timestamp") long time
    ) {
        return verifier.verifiedResponse(context, token, signature, () -> {
            List<Entry> forecasts = outdoorClient.get()
                    .queryParam("Building", building)
                    .queryParam("Tstart", time - 3600)
                    .queryParam("Tend", time + 1800)
                    .send()
                    .readEntity(Message.class)
                    .getEntry();
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
