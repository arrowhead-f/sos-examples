/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.no_need_to_modify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.arrowhead.client.common.no_need_to_modify.exception.BadPayloadException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties({"alwaysMandatoryFields", "id", "metadata", "endOfValidity"})
public class ServiceRegistryEntry extends ArrowheadBase {

  private static final Set<String> alwaysMandatoryFields = new HashSet<>(Arrays.asList("providedService", "provider"));

  //mandatory fields for JSON
  private ArrowheadService providedService;
  private ArrowheadSystem provider;

  //non-mandatory fields for JSON
  private Integer port;
  private String serviceURI;
  private Integer version = 1;
  private boolean udp = false;
  //Time to live in seconds - endOfValidity is calculated from this upon registering and TTL is calculated from endOfValidity when queried
  private long ttl;

  //only for database
  private String metadata;
  private LocalDateTime endOfValidity;

  public ServiceRegistryEntry() {
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider) {
    this.providedService = providedService;
    this.provider = provider;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = provider.getPort();
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, Integer port, String serviceURI) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, Integer port, String serviceURI, Integer version,
                              boolean udp, long ttl, String metadata, LocalDateTime endOfValidity) {
    this.providedService = providedService;
    this.provider = provider;
    this.port = port;
    this.serviceURI = serviceURI;
    this.version = version;
    this.udp = udp;
    this.ttl = ttl;
    this.metadata = metadata;
    this.endOfValidity = endOfValidity;
  }

  public ArrowheadService getProvidedService() {
    return providedService;
  }

  public void setProvidedService(ArrowheadService providedService) {
    this.providedService = providedService;
  }

  public ArrowheadSystem getProvider() {
    return provider;
  }

  public void setProvider(ArrowheadSystem provider) {
    this.provider = provider;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getServiceURI() {
    return serviceURI;
  }

  public void setServiceURI(String serviceURI) {
    this.serviceURI = serviceURI;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public boolean isUdp() {
    return udp;
  }

  public void setUdp(boolean udp) {
    this.udp = udp;
  }

  public long getTtl() {
    return ttl;
  }

  public void setTtl(long ttl) {
    this.ttl = ttl;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getEndOfValidity() {
    return endOfValidity;
  }

  public void setEndOfValidity(LocalDateTime endOfValidity) {
    this.endOfValidity = endOfValidity;
  }

  public Set<String> missingFields(boolean throwException, boolean forDNSSD, Set<String> mandatoryFields) {
    Set<String> mf = new HashSet<>(alwaysMandatoryFields);
    if (mandatoryFields != null) {
      mf.addAll(mandatoryFields);
    }
    Set<String> nonNullFields = getFieldNamesWithNonNullValue();
    mf.removeAll(nonNullFields);
    if (providedService != null) {
      mf = providedService.missingFields(false, forDNSSD, mf);
    }
    if (provider != null) {
      mf = provider.missingFields(false, mf);
    }
    if (throwException && !mf.isEmpty()) {
      throw new BadPayloadException("Missing mandatory fields for " + getClass().getSimpleName() + ": " + String.join(", ", mf));
    }
    return mf;
  }

  public void toDatabase() {
    if (providedService.getServiceMetadata() != null && !providedService.getServiceMetadata().isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> entry : providedService.getServiceMetadata().entrySet()) {
        sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
      }
      metadata = sb.toString().substring(0, sb.length() - 1);
    }

    if (provider.getPort() != null && (port == null || port == 0)) {
      port = provider.getPort();
    }

    endOfValidity = ttl > 0 ? LocalDateTime.now().plusSeconds(ttl) : LocalDateTime.now();
  }

  public void fromDatabase() {
    ArrowheadService temp = providedService;
    providedService = new ArrowheadService();
    providedService.setServiceDefinition(temp.getServiceDefinition());
    providedService.setInterfaces(temp.getInterfaces());

    if (metadata != null) {
      String[] parts = metadata.split(",");
      providedService.getServiceMetadata().clear();
      for (String part : parts) {
        String[] pair = part.split("=");
        providedService.getServiceMetadata().put(pair[0], pair[1]);
      }
    }

    if (port != null && provider.getPort() == null) {
      provider.setPort(port);
    }

    if (endOfValidity != null) {
      if (LocalDateTime.now().isAfter(endOfValidity)) {
        ttl = 0;
      } else {
        ttl = Duration.between(LocalDateTime.now(), endOfValidity).toMinutes() * 60;
      }
    } else {
      ttl = 0;
    }
  }

  @Override
  public String toString() {
    return provider.getSystemName() + ":" + providedService.getServiceDefinition();
  }

}