/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

public class ArrowheadCloud {

  private Long id;
  private String operator;
  private String cloudName;
  private String address;
  private Integer port;
  private String gatekeeperServiceURI;
  private String authenticationInfo;
  private Boolean secure;

  public ArrowheadCloud() {
  }

  public ArrowheadCloud(String operator, String cloudName, String address, Integer port, String gatekeeperServiceURI, String authenticationInfo,
                        Boolean secure) {
    this.operator = operator;
    this.cloudName = cloudName;
    this.address = address;
    this.port = port;
    this.gatekeeperServiceURI = gatekeeperServiceURI;
    this.authenticationInfo = authenticationInfo;
    this.secure = secure;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getCloudName() {
    return cloudName;
  }

  public void setCloudName(String cloudName) {
    this.cloudName = cloudName;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getGatekeeperServiceURI() {
    return gatekeeperServiceURI;
  }

  public void setGatekeeperServiceURI(String gatekeeperServiceURI) {
    this.gatekeeperServiceURI = gatekeeperServiceURI;
  }

  public String getAuthenticationInfo() {
    return authenticationInfo;
  }

  public void setAuthenticationInfo(String authenticationInfo) {
    this.authenticationInfo = authenticationInfo;
  }

  public Boolean isSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrowheadCloud)) {
      return false;
    }

    ArrowheadCloud that = (ArrowheadCloud) o;

    if (operator != null ? !operator.equals(that.operator) : that.operator != null) {
      return false;
    }
    if (cloudName != null ? !cloudName.equals(that.cloudName) : that.cloudName != null) {
      return false;
    }
    if (address != null ? !address.equals(that.address) : that.address != null) {
      return false;
    }
    if (port != null ? !port.equals(that.port) : that.port != null) {
      return false;
    }
    if (gatekeeperServiceURI != null ? !gatekeeperServiceURI.equals(that.gatekeeperServiceURI) : that.gatekeeperServiceURI != null) {
      return false;
    }
    return secure != null ? secure.equals(that.secure) : that.secure == null;
  }

  @Override
  public int hashCode() {
    int result = operator != null ? operator.hashCode() : 0;
    result = 31 * result + (cloudName != null ? cloudName.hashCode() : 0);
    result = 31 * result + (address != null ? address.hashCode() : 0);
    result = 31 * result + (port != null ? port.hashCode() : 0);
    result = 31 * result + (gatekeeperServiceURI != null ? gatekeeperServiceURI.hashCode() : 0);
    result = 31 * result + (secure != null ? secure.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ArrowheadCloud{");
    sb.append("operator='").append(operator).append('\'');
    sb.append(", cloudName='").append(cloudName).append('\'');
    sb.append(", address='").append(address).append('\'');
    sb.append(", port=").append(port);
    sb.append(", gatekeeperServiceURI='").append(gatekeeperServiceURI).append('\'');
    sb.append(", secure=").append(secure);
    sb.append('}');
    return sb.toString();
  }

  public void partialUpdate(ArrowheadCloud other) {
    this.operator = other.getOperator() != null ? other.getOperator() : this.operator;
    this.cloudName = other.getCloudName() != null ? other.getCloudName() : this.cloudName;
    this.address = other.getAddress() != null ? other.getAddress() : this.address;
    this.port = other.getPort() != null ? other.getPort() : this.port;
    this.gatekeeperServiceURI = other.getGatekeeperServiceURI() != null ? other.getGatekeeperServiceURI() : this.gatekeeperServiceURI;
    this.authenticationInfo = other.getAuthenticationInfo() != null ? other.getAuthenticationInfo() : this.authenticationInfo;
    this.secure = other.isSecure() != null ? other.isSecure() : this.secure;
  }
}