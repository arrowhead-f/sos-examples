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
import eu.arrowhead.common.SampleData;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.resources.ArrowheadResource;
import org.joda.time.DateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;

@Path("provider")
@Produces(MediaType.APPLICATION_JSON)
//REST service example
public class OutdoorResource  extends ArrowheadResource {

    public OutdoorResource(ArrowheadServer server) {
        super(server);
    }

    @GET
    public Response getIt(@Context SecurityContext context,
                          @QueryParam("token") String token,
                          @QueryParam("signature") String signature,
                          @DefaultValue("1") @QueryParam("Building") final long building,
                          @DefaultValue("0") @QueryParam("Tstart") final long tstart_in,
                          @DefaultValue("-1") @QueryParam("Tend") final long tend_in
    ) {
        return verifier.verifiedResponse(context, token, signature, () -> {
            final DateTime startOfData = new DateTime(2015, 1, 1, 0, 0);

            long tstart = tstart_in, tend = tend_in;

            if (tend < 0) tend = System.currentTimeMillis() / 1000;
            if (new DateTime(tstart*1000).isBefore(startOfData))
                tstart = startOfData.getMillis()/1000;
            if (new DateTime(tend*1000).isBefore(startOfData))
                tend = startOfData.getMillis()/1000;

            ArrayList<Entry> entries = new ArrayList<>();

            DateTime start = new DateTime(tstart *1000);
            if (start.getSecondOfMinute() > 0 || start.getMinuteOfHour() > 0)
                start = start.plusHours(1);
            start = start.withMinuteOfHour(0).withSecondOfMinute(0);

            for (long ts = start.getMillis()/1000; ts < tend; ts += 3600) {
                Entry entry = new Entry();
                entry.setBuilding(building);
                entry.setTimestamp(ts);
                entry.setOutTemp(SampleData.getOutdoor(ts));
                final float heatLoss = SampleData.getHeatLoss(ts);
                final float heatWater = SampleData.getWaterHeat(ts);
                entry.setTotal(heatLoss + heatWater);
                entry.setWater(heatWater);

                entries.add(entry);
            }

            Message response = new Message();
            if (entries.size() > 0) {
                response.setTstart(entries.get(0).getTimestamp());
                response.setTend(entries.get(entries.size() - 1).getTimestamp());
            }
            response.setEntries(entries);

            return response;
        });
    }

}
