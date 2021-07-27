![MDMI](/files/images/MDMI_Logo.png)
# Welcome to MDMI on GitHub
## Message Transformation Service for Healthcare
The MDMI Project goals are to lower the barriers for implementing healthcare transformations using open standards and open source software. The MDMI Project uses a model driven approach that is compliant with the Object Management Group’s open standard of Model Driven Message Interoperability (MDMI). Visit [OMG.org](https://www.omg.org/spec/MDMI/1.0) for more information.

All software and content on this site are provided under the [Eclipse Public License](/files/Eclipse%20Public%20License%20-%20v%202.0.pdf).

## Hands-on with MDMI Transformations
The components of MDMI message transformations have been provided here on GitHub as well as staged online for immediate use. They include the following:

### Swagger API
An online Swagger implementation is available allowing you to see the structure of the API. Instructions for its use are [here](
https://github.com/MDMI/MDMI-Transformation-Engine/wiki/MDMI-Message-Transformations-Using-Swagger). You can go directly to the site at this URL:  
http://ec2-3-16-40-203.us-east-2.compute.amazonaws.com:8080/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=#/mdmi-engine/transformation  

| 🚩 🚩 🚩 |
| ----------|
| **Connectathon Participants** |  

Please click [here](https://github.com/MDMI/MDMI-Transformation-Engine/wiki/Connectathon) for test messages and a dedicated Swagger implementation.

### Docker Implementation
A Docker image is available to run the application locally - the best option to examine with your own messages. To get the Docker image enter the commands:

*docker pull mdmiservices/cda2fhir*  

Then:

*docker run --name cda2fhir -d -p 5000:8080 mdmiservices/cda2fhir*

A Swagger implementation is accessed at [http://localhost:5000/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=#/mdmi-engine/transformation](http://localhost:5000/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=#/mdmi-engine/transformation)

## MDMI Map Repository
MDMI Maps are MDMI models for a specific healthcare exchange standard. The models are reusable, consumable and computable assets. MDMI Maps are used by the MDMI Transformation Service to transform a source message to a target message. Maps are easily modified or versioned to address changes and variants to any standard or proprietary exchange format.  
[Click here to see example maps](https://github.com/MDMI/MDMI-Transformation-Engine/tree/main/Maps)

## MDMI Workbench
Transformation maps are created using the MDMI Workbench which is implemented as an Eclipse plugin. Setup instructions are documented in this repository's wiki here:  
https://github.com/MDMI/MDMI-Transformation-Engine/wiki/MDMI-Workbench-Installation

## Discussions ![Discussions](/files/images/discuss3-160.png)
The place for all discussions regarding MDMI software and maps is in this repository's [Discussion](https://github.com/MDMI/MDMI-Transformation-Engine/discussions) page. Please feel free to ask questions and provide suggestions for the system. The board is monitored.

## Issues ![Issues](/files/images/issues160.png)
Similarly, when you encounter issues with the software or maps, please enter details in the [Issues](https://github.com/MDMI/MDMI-Transformation-Engine/issues) section of this repository. As are the guidelines in all _good_ bug reporting, please provide enough information and file attachments that the problem can be reproduced.

## License ##
Licensed under the Eclipse Public License - v 2.0 (the "License"); you may not use this file except in compliance with the License. A copy can be obtained at https://www.eclipse.org/legal/epl-2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis, without warranties or conditions of any kind, either express or implied. See the License, sections 5 and 6 for the specific language governing permissions and limitations under the License.
 
## More Information about MDMI
More information regarding MDMI including its specification is located on the Object Management Group's website at https://www.omg.org/mdmi/.  
![OMG](/files/images/OMG_Logo.png)
