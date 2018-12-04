package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.can_be_modified.misc.ClientType;
import eu.arrowhead.client.common.no_need_to_modify.exception.ArrowheadException;
import eu.arrowhead.client.common.no_need_to_modify.exception.ExceptionType;
import eu.arrowhead.client.common.no_need_to_modify.model.IntraCloudAuthEntry;
import eu.arrowhead.client.common.no_need_to_modify.model.OrchestrationStore;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRegistryEntry;

import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public abstract class ArrowheadProvider extends ArrowheadClientMain {
    private final String srBaseUri;
    private ServiceRegistryEntry srEntry = null;

    public ArrowheadProvider(final String[] args, final Class<?>[] classes, final String[] packages) {
        init(ClientType.PROVIDER,args, new HashSet<>(Arrays.asList(classes)), packages);

        String srAddress = props.getProperty("sr_address", "0.0.0.0");
        int srPort = isSecure ?
                props.getIntProperty("sr_secure_port", 8443) :
                props.getIntProperty("sr_insecure_port", 8442);
        srBaseUri = Utility.getUri(srAddress, srPort, "serviceregistry", isSecure, false);
    }

    protected void registerToServiceRegistry(ServiceRegistryEntry srEntry) {
        this.srEntry = srEntry;
        String registerUri = UriBuilder.fromPath(srBaseUri).path("register").toString();
        try {
            Utility.sendRequest(registerUri, "POST", srEntry);
        } catch (ArrowheadException e) {
            if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
                System.out.println("Received DuplicateEntryException from SR, " +
                        "sending delete request and then registering again.");
                unregisterFromServiceRegistry();
                Utility.sendRequest(registerUri, "POST", srEntry);
            } else {
                throw e;
            }
        }
        System.out.println("Registering service is successful!");
    }

    protected void unregisterFromServiceRegistry() {
        if (srEntry != null) {
            String removeUri = UriBuilder.fromPath(srBaseUri).path("remove").toString();
            Utility.sendRequest(removeUri, "PUT", srEntry);
            srEntry = null;
            System.out.println("Removing service is successful!");
        }
    }

    @Override
    protected void shutdown() {
        unregisterFromServiceRegistry();
        super.shutdown();
    }

    protected void registerToAuthorization(IntraCloudAuthEntry authEntry) {
        String authAddress = props.getProperty("auth_address", "0.0.0.0");
        int authPort = isSecure ?
                props.getIntProperty("auth_secure_port", 8445) :
                props.getIntProperty("auth_insecure_port", 8444);
        String authUri = Utility.getUri(authAddress, authPort, "authorization/mgmt/intracloud",
                isSecure, false);
        Utility.sendRequest(authUri, "POST", authEntry);
        System.out.println("Authorization registration is successful!");
    }

    protected void registerToStore(List<OrchestrationStore> storeEntry) {
        String orchAddress = props.getProperty("orch_address", "0.0.0.0");
        int orchPort = props.getIntProperty("orch_port", 8440);
        String orchUri = Utility.getUri(orchAddress, orchPort, "orchestrator/mgmt/store",
                false, false);
        Utility.sendRequest(orchUri, "POST", storeEntry);
        System.out.println("Store registration is successful!");
    }
}
