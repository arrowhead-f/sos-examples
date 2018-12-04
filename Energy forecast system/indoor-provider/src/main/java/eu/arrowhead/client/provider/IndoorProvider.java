/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.no_need_to_modify.ArrowheadProps;
import eu.arrowhead.client.common.no_need_to_modify.ArrowheadProvider;
import eu.arrowhead.client.common.no_need_to_modify.Utility;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRegistryEntry;

public class IndoorProvider extends ArrowheadProvider {

  public static void main(String[] args) {
    new IndoorProvider(args);
  }

  private IndoorProvider(String[] args) {
    super(args,
            new Class[] {IndoorResource.class},
            new String[] {"eu.arrowhead.client.common"});

    ServiceRegistryEntry srEntry = ArrowheadProps.getServiceRegistryEntry(props, baseUri, isSecure, base64PublicKey);
    System.out.println("Service Registry Entry: " + Utility.toPrettyJson(null, srEntry));
    registerToServiceRegistry(srEntry);

    listenForInput();
  }

}
