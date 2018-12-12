# Mini example project on how to define and then implement Arrowhead Services and Systems
## Educational material from Bilbao workshop

There are two examples in this folder with two scenarios. 

_Prerequisites_: mandatory core systems are installed locally, or you may use the public testbeds. 
Core version: Arrowhead 4.1.0

You can install the core systems using the [Debian installation packages.](https://github.com/arrowhead-f/core-java/blob/master/documentation/Debian%20Packages/DEBIAN-INSTALL.md) You can also deploy and configure them manually. 

You can find the latest releases [here](https://github.com/arrowhead-f/core-java/releases). 

**1.** setRPM service

We have a motor we want to be able to control its current RPM. This _setRPM_ can be an Arrowhead service. 
We choose a push-typed service implementation, so the motor will be the service provider system, and controllers will be the consumers of this _setRPM_ service.  
The implementation will be based on HTTP REST, we want it to be a secure service (TLS enabled) and also have the Arrowhead token included in the service interface implementation (for admission control). 
As per the general Arrowhead design, my PLC controller instance will be looking for this setRPM service through orchestration in order to be able to set the RPM of a given motor instance. 

  * Arrowhead token enabled secure service
  
  * a simple, but custom JSON messages-based interface (no ontology or standard-based semantic profile)
  
  * the service invocation will be an HTTPS POST  made to the URL with "Content-Type: application/json" header:
    ```
    https://providerIP:port/serviceURI/rpm?token=xxxxx&signature=xxxxx
    ```
  where the providerIP, port and serviceURI (baseURL) will be provider instance specific. The _token_ and _signature_ path parameters will be how this service interface design implements the Arrowhead token. 
  
  * expected request payload:
  ```
  {"RPM" : 10000}
  ```
  
  *expected response: HTTP 200 OK 
  ```
  {"currentRPM": 10000}
  ```
  
My systems will bear the following details, based on the Arrowhead naming convention: 

  * hosting service provider system instance details: systemName="motor123-controller-0", port= 8080, baseURL = "/controller"
  * consumer system instance details: systemName = "boschPLC-RPMloop-0"
  
These will be the systemNames that will be registered in the ServiceRegistry and Authoriz
 Based on these details, the SD, IDD and CP documents of Arrowhead are to be written, and their filenames must be: 
 
  * SD filename: "setRPM"
  
  * IDD filename: "RPM-JSON-CUSTOM-HTTP-SECURE_AC"
  
 Based on this design, the implementation is basically a modification of the provider and consumer skeletons. These can be found in this folder. 
 
 _Since this is a secure service implementation, the consumer and provider systems need to have proper, Arrowhead-issued certificates and their public keys registered in the Authorization System, see point 3._ 

**2.** HumidityWSN service

I want to design an Arrowhead service which will enable me to query the humidity sensor values from a Wireless Sensor Network (WSN). 
This should be a pull-typed service: simple prototype humidity sensor network can be queried, similar to the temperature, but from a WSN through its gateway.  This service will be implemented by the gateway of the WSN. 


  * insecure (plain HTTP) service, no ontology - simple string interface
  
  * simple HTTP GET request to: 
    ```
    http://providerIP:port/serviceURI/1/humidity" (where the /1/ is the WSN nodeID, part of the interface design)
    ```
    
  * expected response: HTTP 200 OK with message: 
  ```
  40%
  ```
  
This renders the SD and  names to be the following: 
  * SD filename: "Humidity"
  * IDD filename: "Humidity-String-CUSTOM-HTTP-NONE"
  
I choose my demo systems to have details:  
  * hosting system details: systemName = "wsnGateway144-mediator-0", port = 8080, baseURL ="/values"
  * consumer system details: systemName = "laptop4-dashboard-0"

**3.** Setting things up

**3.1.** Generating certificates
The first service (setRPM) is secure, and therefore the systems (provider and consumer) need proper certificates. 
Certificate generation at the moment can only happen manually. 

Please read [this guide](https://github.com/arrowhead-f/core-java/wiki/Creating-Application-System-Certificates) on how to create Arrowhead compliant certificates.

The second service implementation is insecure, so no certificates are needed for those systems hosting and consuming the HumidityWSN service. 

**3.2.** Setting up the core: adding authorization rules
In order for the consumers to be able to access the providers, authorization rules must be in place. 
Authorization rules can be added using the management REST interface of the Authorization System. 

The endpoint for this interface is at /authorization/mgmt/intracloud and /authorization/mgmt/intercloud for intra-cloud and inter-cloud rules respectively, **using POST HTTP method**. The full URL for example can be http://0.0.0.0:8444/authorization/mgmt/intracloud.

**3.2.1.** Adding the humidity service intra-cloud authorization rules:
```
{
  "consumer" : {
    "systemName" : "laptop4-dashboard-0",
    "address" : "localhost",
    "port" : 8080
  },
  "providerList" : [ {
    "systemName" : "wsnGateway144-mediator-0",
    "address" : "0.0.0.0",
    "port" : 8080
  } ],
  "serviceList" : [ {
    "serviceDefinition" : "Humidity",
    "interfaces" : [ "String-CUSTOM-HTTP-NONE" ],
    "serviceMetadata" : { }
  } ]
}
```

**3.2.2** Adding the RPM service intra-cloud authorization rules:
```
{
  "consumer" : {
    "systemName" : "boschPLC-RPMloop-0",
    "address" : "localhost",
    "port" : 8080
  },
  "providerList" : [ {
    "systemName" : "motor123-controller-0",
    "address" : "0.0.0.0",
    "port" : 8080
  } ],
  "serviceList" : [ {
    "serviceDefinition" : "RPM",
    "interfaces" : [ "JSON-CUSTOM-HTTP-SECURE_AC" ],
    "serviceMetadata" : { }
  } ]
}
```

