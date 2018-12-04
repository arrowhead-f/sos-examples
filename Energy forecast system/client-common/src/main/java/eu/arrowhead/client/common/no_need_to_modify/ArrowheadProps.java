package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.no_need_to_modify.misc.TypeSafeProperties;
import eu.arrowhead.client.common.no_need_to_modify.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ArrowheadProps {
    public static ArrowheadService getService(Properties props, boolean isSecure) {
        String serviceDef = props.getProperty("service_name");
        String interfaceList = props.getProperty("interfaces");
        List<String> interfaces = new ArrayList<>();
        if (interfaceList != null && !interfaceList.isEmpty()) {
            interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
        }
        Map<String, String> metadata = getMetadata(props, isSecure);
        return new ArrowheadService(serviceDef, interfaces, metadata);
    }

    public static Map<String, String> getMetadata(Properties props, boolean isSecure) {
        Map<String, String> metadata = new HashMap<>();
        String metadataString = props.getProperty("metadata");
        if (metadataString != null && !metadataString.isEmpty()) {
            String[] parts = metadataString.split(",");
            for (String part : parts) {
                String[] pair = part.split("-");
                metadata.put(pair[0], pair[1]);
            }
        }
        if (isSecure && !metadata.containsKey("security")) {
            metadata.put("security", "token");
        }
        return metadata;
    }

    public static ArrowheadSystem getProvider(Properties props, String baseUri, boolean isSecure, String base64PublicKey) {
        URI uri;
        try {
            uri = new URI(baseUri);
        } catch (URISyntaxException e) {
            throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
        }

        ArrowheadSystem provider;
        if (isSecure) {
            String secProviderName = props.getProperty("secure_system_name");
            provider = new ArrowheadSystem(secProviderName, uri.getHost(), uri.getPort(), base64PublicKey);
        } else {
            String insecProviderName = props.getProperty("insecure_system_name");
            provider = new ArrowheadSystem(insecProviderName, uri.getHost(), uri.getPort(), null);
        }

        return provider;
    }

    public static ArrowheadSystem getConsumer(Properties props) {
        String consumerName = props.getProperty("consumer_name");
        String consumerAddress = props.getProperty("consumer_address");
        String consumerPK = props.getProperty("consumer_public_key");
        return new ArrowheadSystem(consumerName, consumerAddress, 0, consumerPK);
    }

    public static List<OrchestrationStore> getStoreEntry(Properties props, String baseUri, boolean isSecure, String base64PublicKey) {
        return Collections.singletonList(new OrchestrationStore(
                getService(props, isSecure),
                getConsumer(props),
                getProvider(props, baseUri, isSecure, base64PublicKey),
                0,
                false));
    }

    public static IntraCloudAuthEntry getAuthEntry(String base64PublicKey, String baseUri, boolean isSecure, TypeSafeProperties props) {
        return new IntraCloudAuthEntry(
                getConsumer(props),
                Collections.singletonList(getProvider(props, baseUri, isSecure, base64PublicKey)),
                Collections.singletonList(getService(props, isSecure)));
    }

    public static ServiceRegistryEntry getServiceRegistryEntry(Properties props, String baseUri, boolean isSecure, String base64PublicKey) {
        String serviceUri = props.getProperty("service_uri");
        return new ServiceRegistryEntry(
                getService(props, isSecure),
                getProvider(props, baseUri, isSecure, base64PublicKey),
                serviceUri);
    }
}
