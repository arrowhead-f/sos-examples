package eu.arrowhead.client.provider;

import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.OrchestrationFlags;
import eu.arrowhead.common.model.ServiceRequestForm;

public class EnergyForecastUtils {
    static HttpClient createClient(OrchestrationClient orchestrationClient, ArrowheadSystem me, String service) {
        final boolean secure = orchestrationClient.isSecure();
        final ArrowheadSecurityContext securityContext = orchestrationClient.getSecurityContext();

        final ServiceRequestForm srf = new ServiceRequestForm.Builder(me)
                .requestedService(service, "json", secure)
                .flag(OrchestrationFlags.Flags.OVERRIDE_STORE, true)
                .flag(OrchestrationFlags.Flags.PING_PROVIDERS, false)
                .flag(OrchestrationFlags.Flags.METADATA_SEARCH, true)
                .flag(OrchestrationFlags.Flags.ENABLE_INTER_CLOUD, false)
                .build();

        return new HttpClient(new OrchestrationStrategy.Always(orchestrationClient, srf), securityContext);
    }
}
