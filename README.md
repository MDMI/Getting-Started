![MDMI](/files/MDMI_Logo.png)
# Welcome to MDMI on GitHub
## Message Transformation Service for Healthcare
The MDMI Project goals are to lower the barriers for implementing healthcare transformations using open standards and open source software. The MDMI Project uses a model driven approach that is compliant with the Object Management Group’s open standard of Model Driven Message Interoperability (MDMI). Visit [OMG.org](https://www.omg.org/spec/MDMI/1.0) for more information.

All software and content on this site are provided under the [Eclipse Public License](/files/Eclipse%20Public%20License%20-%20v%202.0.pdf).

## Hands-on with MDMI Transformations
The components of MDMI message transformations have been provided here on GitHub as well as staged online for immediate use. They include the following:

### Swagger API
An online Swagger implementation is available allowing you to see the structure of the API. Instructions for its use are [here](/files/SwaggerInstructions.pdf). You can go directly to the site at this URL:  
http://ec2-3-16-40-203.us-east-2.compute.amazonaws.com:8080/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=#/mdmi-engine/transformation  

### Docker Implementation
A Docker image is available to run the application locally - the best option to examine with your own messages. To get the Docker image enter the commands:

*docker pull mdmiservices/cda2fhir*  

Then:

*docker run --name cda2fhir -d -p 5000:8080 mdmiservices/cda2fhir*

A Swagger implementation is accessed at http://localhost:5000/swagger-ui/i

## MDMI Map Repository
MDMI Maps are MDMI models for a specific healthcare exchange standard. The models are reusable, consumable and computable assets. MDMI Maps are used by the MDMI Transformation Service to transform a source message to a target message. Maps are easily modified or versioned to address changes and variants to any standard or proprietary exchange format.  
[Click here to see the Published Maps](https://github.com/MDMI/PublishedMaps)

## MDMI Workbench
Transformation maps are created using the MDMI Workbench which is implemented as an Eclipse plugin.

### New Eclipse Installations
You can find the Eclipse IDE here https://www.eclipse.org/downloads/.

For the MDMI Editor, download and unzip the its setup file [from here](/files/MDMI201909.zip) placing it in the same directory as the Eclipse installer, then run the installer. Click the "hamburger" menu of the installer.  

![HamburgerMenu](/files/hamburgerMenu.png)  

From that menu, select **ADVANCED MODE…**

![AdvancedMode](/files/advMode.png)  

On the next window click the green plus sign.

![productPlusSign](/files/productPlusSign.png)  

In the ensuing **Add User Products** popup click the **Browse File System** button to select the MDMI setup file. Continue with the normal installation process.

### Existing Eclipse Installations
TBD  
<< insert instructuions here >>
 
## More Information about MDMI
More information regarding MDMI including its specification is located on the Object Management Group's website at https://www.omg.org/mdmi/.  
![OMG](/files/OMG_Logo.png)
