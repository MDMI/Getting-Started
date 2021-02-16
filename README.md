# Welcome to the wonderful world of Model Driven Message Interopability 

We have serveral runtime options to choose from so please select wisely

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

## Fourth option - Eclipse IDE
For you MDMI pleasure we have developed extensions to the eclipse environment
It can be install using the eclipse installer and this set up file
<< insert eclipse installer instructions>> and setup file


