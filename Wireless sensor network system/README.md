1. Set RPM Service, push type (consumer is the data source/controller, pushing its RPM setting to a motor)
-  Arrowhead token enabled service, custom JSON interface (no ontology or standard-based semantic profile)
- SD filename: "RPM", IDD filename: "RPM-JSON-CUSTOM-HTTP-SECURE_AC"
- hosting system details: systemName="motor123-controller-0", port= 8080, baseURL = "/controller"
- consumer system details: systemName = "boschPLC-RPMloop-0"
- HTTPS POST to ".../rpm?token=asdadsads&signature=asdsadasdsa"  with "Content-Type: application/json" header
- expected request payload: {"RPM" : 10000}
- expected response: 200 OK, {"currentRPM": 10000}

2. Humidity service, pull type (simple prototype humidity sensor network can be queried, similar to the temperature, but from a WSN through a gateway)
- insecure (plain HTTP) service, no ontology - simple string interface
- SD filename: "Humidity", IDD filename: "Humidity-String-CUSTOM-HTTP-NONE"
- hosting system details: systemName = "wsnGateway144-mediator-0", port = 8080, baseURL ="/values"
- consumer system details: systemName = "laptop4-dashboard-0"
- simple HTTP GET request to ".../1/humidity" (where the /1/ is the WSN nodeID)
- expected response: 200 OK, "40%" 
