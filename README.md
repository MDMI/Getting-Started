![Logo](/files/MDMI_Logo.png)
# Welcome to MDMI on GitHub
## Message Transformation Service for Healthcare
The MDMI Project goals are to lower the barriers for implementing healthcare transformations using open standards and open source software. The MDMI Project uses a model driven approach that is compliant with the Object Management Group’s open standard of Model Driven Message Interoperability (MDMI). Visit [OMG.org](https://www.omg.org/spec/MDMI/1.0) for more information.

All software and content on this site are provided under the Eclipse license.

## First option - for your interested lookie-loos we have a web page UI
<< insert link to ui here >
This option is for SME and other general business types who want to make sure the software works

## Second option - for your adventurous types - we have a swagger api
This option allows you to see the structure of the api but beware it is not designed for actual PMI
<< insert link to swagger >>

## Third Option - docker image
We have a docker image where you can run the application locally - best option to test on your own messages 

docker pull mdmiservices/cda2fhir

docker run --name cda2fhir -d -p 5000:8080 mdmiservices/cda2fhir

and to acess the local swagger 
http://localhost:5000/swagger-ui/i

## MDMI Map Repository
MDMI Maps are MDMI models for a specific healthcare exchange standard. The models are reusable, consumable and computable assets. MDMI Maps are used by the MDMI Transformation Service to transform a source message to a target message. Maps are easily modified or versioned to address changes and variants to any standard or proprietary exchange format.  
[Click here to see the Published Maps](https://github.com/MDMI/PublishedMaps)

## MDMI Editor
Transformation maps are created using the MDMI Editor which is implemented as an Eclipse plugin.

### New Eclipse Installations
You can find the Eclipse IDE here https://www.eclipse.org/downloads/.
Download and unzip the editor’s setup file [here](/files/MDMI201909.zip) placing it in the same directory as the Eclipse installer, then run the installer. Click the hamburger menu of the installer.
![HamburgerMenu](/files/hamburgerMenu.png)
From that menu, select **ADVANCED MODE…**
![AdvancedMode](/files/advMode.png)
After the prompt about storing the installer you will get the **Product** dialog which will show *MDMIWorkbench*. Select it and continue with the normal installation process.

### Existing Eclipse Installations
TBD  
<< insert instructuions here >>
 
## More Information about MDMI
More information regarding MDMI including its specification is located on the Object Management Group's website at https://www.omg.org/mdmi/.

