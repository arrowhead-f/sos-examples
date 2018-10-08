/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.consumer;

import eu.arrowhead.client.common.can_be_modified.model.Message;
import eu.arrowhead.client.common.can_be_modified.model.TemperatureReadout;
import eu.arrowhead.client.common.no_need_to_modify.ArrowheadConsumer;
import eu.arrowhead.client.common.no_need_to_modify.Orchestrator;
import eu.arrowhead.client.common.no_need_to_modify.Utility;
import eu.arrowhead.client.common.no_need_to_modify.model.ArrowheadService;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRequestForm;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OutdoorConsumer extends ArrowheadConsumer {
    private OutdoorConsumer(String[] args) {
        // SystemName can be an arbitrarily chosen name, which makes sense for the use case.
        super(args, "client1");

        // Start a timer, to measure the speed of the Core Systems and the provider application system.
        long startTime = System.currentTimeMillis();

        // Compile the payload, that needs to be sent to the Orchestrator
        ServiceRequestForm srf = compileSRF();

        // Sending the orchestration request and parsing the response
        String providerUrl = new Orchestrator(isSecure).sendOrchestrationRequest(srf).toString();

        // Connect to the provider, consuming its service
        consumeService(providerUrl);

        // Printing out the elapsed time during the orchestration and service consumption
        long endTime = System.currentTimeMillis();
        System.out.println("Orchestration and Service consumption response time: " + Long.toString(endTime - startTime));
    }

    public static void main(String[] args) {
        new OutdoorConsumer(args);
    }

    // Compiles the payload for the orchestration request
    // THIS METHOD SHOULD BE MODIFIED ACCORDING TO YOUR NEEDS
    private ServiceRequestForm compileSRF() {
        // You can put any additional metadata you look for in a Service here (key-value pairs)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("unit", "celsius");
        if (isSecure) {
            // This is a mandatory metadata when using TLS, do not delete it
            metadata.put("security", "token");
        }

    /*
      ArrowheadService: serviceDefinition (name), interfaces, metadata
      Interfaces: supported message formats (e.g. JSON, XML, JSON-SenML),
      a potential provider has to have at least 1 match,
      so the communication between consumer and provider can be facilitated.
     */
        ArrowheadService service = new ArrowheadService("Outdoor", Collections.singletonList("json"), metadata);

        // Some of the orchestrationFlags the consumer can use, to influence the orchestration process
        Map<String, Boolean> orchestrationFlags = new HashMap<>();
        // When true, the orchestration store will not be queried for "hard coded" consumer-provider connections
        orchestrationFlags.put("overrideStore", true);
        // When true, the Service Registry will ping every potential provider, to see if they are alive/available on the
        // network
        orchestrationFlags.put("pingProviders", false);
        // When true, the Service Registry will only providers with the same exact metadata map as the consumer
        orchestrationFlags.put("metadataSearch", true);
        // When true, the Orchestrator can turn to the Gatekeeper to initiate interCloud orchestration, if the Local Cloud
        // had no adequate provider
        orchestrationFlags.put("enableInterCloud", true);

        // Build the complete service request form from the pieces, and return it
        return buildServiceRequestForm(service, orchestrationFlags);
    }

    // THIS METHOD SHOULD BE MODIFIED ACCORDING TO YOUR USE CASE
    private static void consumeService(String providerUrl) {
        Message readout = null;

    /*
      Sending request to the provider, to the acquired URL. The method type and payload should be known beforehand.
      If needed, compile the request payload here, before sending the request.
      Supported method types at the moment: GET, POST, PUT, DELETE

      Parsing the response from the provider here. This code prints an error message, if the answer is not in the
      expected JSON format, but custom error handling can also be implemented here. For example the Orchestrator will
      send back a JSON with the structure of the eu.arrowhead.client.common.exception.ErrorMessage class, and the errors
      from the Orchestrator are parsed this way.
     */
        try {
            readout = Utility.requestEntity("GET", providerUrl, null, Message.class);
            System.out.println("Provider Response payload: " + Utility.toPrettyJson(null, readout));
        } catch (RuntimeException ignored) {
        }

        if (readout != null) {
            final String string = "Got " + readout.getEntry().size() + " entries.";
            System.out.println(string);
            JLabel label = new JLabel(string);
            label.setFont(new Font("Arial", Font.BOLD, 18));
            JOptionPane.showMessageDialog(null, label,"Provider Response", JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
