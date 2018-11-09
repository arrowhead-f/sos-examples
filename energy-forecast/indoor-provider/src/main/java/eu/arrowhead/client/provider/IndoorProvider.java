/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.common.api.ArrowheadClient;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.model.ServiceRegistryEntry;

public class IndoorProvider extends ArrowheadClient {

  public static void main(String[] args) {
    new IndoorProvider(args).start(true);
  }

  private IndoorProvider(String[] args) {
    super(args);
  }

  @Override
  protected void onStart(ArrowheadSecurityContext securityContext) {
    final ArrowheadServer server = ArrowheadServer
            .createFromProperties(securityContext)
            .addResources(IndoorResource.class)
            .start();

    ServiceRegistryClient
            .createFromProperties(securityContext)
            .register(ServiceRegistryEntry.createFromProperties(server));
  }

  @Override
  protected void onStop() {

  }

}
