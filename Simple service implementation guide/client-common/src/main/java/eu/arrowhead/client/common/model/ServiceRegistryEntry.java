/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

import java.time.LocalDateTime;

public class ServiceRegistryEntry {

  private Long id;
  private ArrowheadService providedService;
  private ArrowheadSystem provider;
  private String serviceUri;
  private Boolean udp;
  private LocalDateTime endOfValidity;
  private Integer version = 1;

  public ServiceRegistryEntry() {
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceUri) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceUri = serviceUri;
  }


  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceUri, boolean udp, LocalDateTime endOfValidity,
                              int version) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceUri = serviceUri;
    this.udp = udp;
    this.endOfValidity = endOfValidity;
    this.version = version;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getServiceUri() {
    return serviceUri;
  }

  public void setServiceUri(String serviceUri) {
    this.serviceUri = serviceUri;
  }

  public Boolean isUdp() {
    return udp;
  }

  public void setUdp(Boolean udp) {
    this.udp = udp;
  }

  public LocalDateTime getEndOfValidity() {
    return endOfValidity;
  }

  public void setEndOfValidity(LocalDateTime endOfValidity) {
    this.endOfValidity = endOfValidity;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceRegistryEntry)) {
      return false;
    }

    ServiceRegistryEntry that = (ServiceRegistryEntry) o;

    if (providedService != null ? !providedService.equals(that.providedService) : that.providedService != null) {
      return false;
    }
    if (provider != null ? !provider.equals(that.provider) : that.provider != null) {
      return false;
    }
    if (serviceUri != null ? !serviceUri.equals(that.serviceUri) : that.serviceUri != null) {
      return false;
    }
    return version != null ? version.equals(that.version) : that.version == null;
  }

  @Override
  public int hashCode() {
    int result = providedService != null ? providedService.hashCode() : 0;
    result = 31 * result + (provider != null ? provider.hashCode() : 0);
    result = 31 * result + (serviceUri != null ? serviceUri.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ServiceRegistryEntry{");
    sb.append("providedService=").append(providedService);
    sb.append(", provider=").append(provider);
    sb.append('}');
    return sb.toString();
  }
}