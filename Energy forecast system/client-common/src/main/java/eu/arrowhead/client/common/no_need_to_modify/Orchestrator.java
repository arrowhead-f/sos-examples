package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.no_need_to_modify.exception.ArrowheadException;
import eu.arrowhead.client.common.no_need_to_modify.misc.TypeSafeProperties;
import eu.arrowhead.client.common.no_need_to_modify.model.ArrowheadSystem;
import eu.arrowhead.client.common.no_need_to_modify.model.OrchestrationResponse;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRequestForm;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class Orchestrator {
    private final TypeSafeProperties props = Utility.getProp("app.properties");
    private final String orchestratorUrl;

    public Orchestrator(boolean isSecure) {
        orchestratorUrl = getOrchestratorUrl(isSecure);
    }

    /**
     * Sends the orchestration request to the Orchestrator, and compiles the URL for the first provider received from
     * the OrchestrationResponse
     */
    public UriBuilder sendOrchestrationRequest(ServiceRequestForm srf) {
        //Sending a POST request to the orchestrator (URL, method, payload)
        Response postResponse = Utility.sendRequest(orchestratorUrl, "POST", srf);
        //Parsing the orchestrator response
        OrchestrationResponse orchResponse = postResponse.readEntity(OrchestrationResponse.class);
        System.out.println("Orchestration Response payload: " + Utility.toPrettyJson(null, orchResponse));
        if (orchResponse.getResponse().isEmpty()) {
            throw new ArrowheadException("Orchestrator returned with 0 Orchestration Forms!");
        }

        //Getting the first provider from the response
        ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
        String serviceURI = orchResponse.getResponse().get(0).getServiceURI();
        //Compiling the URL for the provider
        UriBuilder ub = UriBuilder.fromPath("").host(provider.getAddress()).scheme("http");
        if (serviceURI != null) {
            ub.path(serviceURI);
        }
        if (provider.getPort() > 0) {
            ub.port(provider.getPort());
        }
        if (orchResponse.getResponse().get(0).getService().getServiceMetadata().containsKey("security")) {
            ub.scheme("https");
            ub.queryParam("token", orchResponse.getResponse().get(0).getAuthorizationToken());
            ub.queryParam("signature", orchResponse.getResponse().get(0).getSignature());
        }
        System.out.println("Received provider system URL: " + ub.toString());
        return ub;
    }

    /**
     * Gets the correct URL where the orchestration requests needs to be sent (from app.properties config file +
     * command line argument)
     * @param isSecure
     */
    private String getOrchestratorUrl(boolean isSecure) {
        String orchAddress = props.getProperty("orch_address", "0.0.0.0");

        if (isSecure) {
            int orchSecurePort = props.getIntProperty("orch_secure_port", 8441);
            return Utility.getUri(orchAddress, orchSecurePort, "orchestrator/orchestration", true, false);
        } else {
            int orchInsecurePort = props.getIntProperty("orch_insecure_port", 8440);
            return Utility.getUri(orchAddress, orchInsecurePort, "orchestrator/orchestration", false, false);
        }
    }
}
