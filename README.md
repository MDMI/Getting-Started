![MDMI](/files/images/MDMI_Logo.png)
# Welcome to MDMI on GitHub
## Message Transformation Service for Healthcare
The MDMI Project goals are to lower the barriers for implementing healthcare transformations using open standards and open source software. The MDMI Project uses a model driven approach that is compliant with the Object Management Group’s open standard of Model Driven Message Interoperability (MDMI). Visit [OMG.org](https://www.omg.org/spec/MDMI/1.0) for more information.

All software and content on this site are provided under the [Eclipse Public License](/files/Eclipse%20Public%20License%20-%20v%202.0.pdf).

## Hands-on with MDMI Transformations
The components of MDMI message transformations have been provided here on GitHub as well as staged online for immediate use. To examine message transformations for yourself, we suggest these:

1. Postman - a popular tool for API development. A standalone application available at https://www.postman.com/
2. Swagger - another tool for developing and testing REST APIs. Swagger is a hosted application running on the same server as the MDMI Transformation Engine.
<!--
3. cURL - a command line tool for transferring data using various network protocols. It is included in many Linux distributions and is available at https://curl.se/
-->

The MDMI Transformation Service is provided as a Docker image that can be downloaded for local use. However, a copy is hosted so that you may use the service without setting up the Docker infrastructure. The hosted copy is referenced below.

### Docker Implementation
(Skip this step if using the hosted instance of the MDMI Transformation Service.)

When running locally, download and run the MDMI Docker image using the commands:

`docker pull mdmiservices/transformation`  

Then:

`docker run --name cda2fhir -d -p 5000:8080 mdmiservices/transformation`

### API Test (Postman) ###
A set of API requests is provided for ease of use. Download this zip file, [MDMI_Demo.zip,](https://github.com/MDMI/Getting-Started/blob/main/files/MDMI_Demo.zip) and expand it. The resultant file is *MDMI_Demo.json*.

#### Using the Prepackaged Requests ####
After installing Postman, download and expand the zip file as described above. From within Postman, click the **Import** button at the top of the left pane and select this file. It will create a "collection" named **MDIX Demonstration**. This provides six requests:
- GET Get  
- POST CDAtoFHIR  
- POST FHIR2CDA  
- POST V2toFHIR  
- POST SBHAtoFHIR 
- POST SBHAtoCDA  

#### Running Postman ####
The POST requests are used for message transformation. For example, to transform a CDA message, click on **GET CDAtoFHIR** from the left pane. 

![](/files/images/Postman2.png)

In the right pane, click on **Body** as shown.

![CDAtoFHIR](/files/images/Postman1.png)

From there click the **Select Files** button and enter your message file.

![](/files/images/Postman3.png)

At this point you can run by clicking the **Send** button in the upper right of the window. The resultant message will appear at the bottom of the right pane.
<!--
#### Creating POST Requests ####
In Postman, create a new request with this information:  
**Type:** POST  
**URL:** http://localhost:5000/mdmi/transformation  
**Params Keys**  
*source:* CDAR2.ContinuityOfCareDocument (MDMI source map)   
*target:* FHIRR4JSON.MasterBundle (MDMI target map)  
**Body Key**  
*message:* your CDA source message file  
-->
### Swagger API
An online Swagger implementation is available allowing you to see the structure of the API. Instructions for its use are [here](
https://github.com/MDMI/Getting-Started/wiki/MDMI-Message-Transformations-Using-Swagger). You can go directly to the site at this URL:  
http://ec2-18-117-181-57.us-east-2.compute.amazonaws.com:8080/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=  

When running locally, a Swagger implementation is accessed at http://localhost:5000/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=#/mdmi-engine/transformation

## MDMI Map Repository
MDMI Maps are MDMI models for a specific healthcare exchange standard. The models are reusable, consumable and computable assets. MDMI Maps are used by the MDMI Transformation Service to transform a source message to a target message. Maps are easily modified or versioned to address changes and variants to any standard or proprietary exchange format.  
[Click here to see example maps](https://github.com/MDMI/Getting-Started/tree/main/Maps)

## MDMI Workbench
Transformation maps are created using the MDMI Workbench which is implemented as an Eclipse plugin. Setup instructions are documented in this repository's wiki here:  
https://github.com/MDMI/Getting-Started/wiki/MDMI-Workbench-Installation

## Discussions ![Discussions](/files/images/discuss3-160.png)
The place for all discussions regarding MDMI software and maps is in this repository's [Discussion](https://github.com/MDMI/Getting-Started/discussions) page. Please feel free to ask questions and provide suggestions for the system. The board is monitored.

## Issues ![Issues](/files/images/issues160.png)
Similarly, when you encounter issues with the software or maps, please enter details in the [Issues](https://github.com/MDMI/Getting-Started/issues) section of this repository. As are the guidelines in all _good_ bug reporting, please provide enough information and file attachments that the problem can be reproduced.

## License ##
Licensed under the Eclipse Public License - v 2.0 (the "License"); you may not use this file except in compliance with the License. A copy can be obtained at https://www.eclipse.org/legal/epl-2.0/.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" basis, without warranties or conditions of any kind, either express or implied. See the License, sections 5 and 6 for the specific language governing permissions and limitations under the License.
 
## More Information about MDMI
More information regarding MDMI including its specification is located on the Object Management Group's website at https://www.omg.org/mdmi/.  
![OMG](/files/images/OMG_Logo.png)
