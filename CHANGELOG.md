# CHANGELOG version 2.0.0

## General Changes 

* CI/CD deployment
* Better and more efficient workflow
* Better JavaDoc
* Added Documentation
* Cleaned up code

## Added Features

* Train/Test data split
* Closed World and Open World Assumption
* Allowing RDF files as ABox
* Uses Openllet Reasoner for OWA
* Added strict option to assure minimum examples
* Added Evaluation script
* Added BaseLine System 
* SPARQL infers super types directly now
* Setting the ABox triple retrieval query limit
* Allowing a positive and negative query limit for Instance Retrieval
* A random pick of Concepts depending on seed
* Allowing random negation mutations of concepts during generation
* Allowing lateral creation of concepts.

## Removed Features

* Removed: Adding Individuals not in any learning problem
* Removed: endpointInfersRules parameter removed, as SPARQL query now directly reasons types.

## Issues fixed

* Concept creator didn't work correctly (Concepts of depth>1 couldn't be created)