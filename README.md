# JavaCodeEcorification
This is a proof of concept for the automatic integration of Ecore functionality into Java code. We call this process Ecorification. The goal is to integrate Ecore functionality into Java code while preserving all of its original functionality. Preserving the original functionality of the Java code requires retaining the interfaces which are offered by the modules of the code and also retaining all internal functionality of the code. That means the product of the Ecorification is the original code, enriched with the desired Ecore functionality. It can be used exactly as before, but it also uses the modeling infrastructure and implements all interfaces for Ecore-based tooling.

The basic idea of the Ecorification is to find an Ecore representation of the Java code, which is used to integrate its Ecore functionality into the Java code. The Ecore representation is obtained by creating an Ecore metamodel that represents the Java code as closely as possible (with the help of the [EcoreMetamodelExtraction project](https://github.com/tsaglam/EcoreMetamodelExtraction)). We use the Eclipse Modeling Framework to generate Ecore model code from the Ecore metamodel. We then interlace both the original Java code and the generated Ecore model code. This can be achieved by utilizing the separation of interface and implementation in the Ecore count to mount the original code into the super relation hierarchy of the model code. The combination of both codes then contains the implementation details of the original code and the Ecore functionality of the model code. This way, the Java Code Ecorification allows the integration of Ecore functionality.

This process is depicted in the following diagram:
<p align="center"> 
<img alt="The Ecorification Process" src="https://www.lucidchart.com/publicSegments/view/19c5bae0-9aed-4e40-b805-dba516a92472/image.png" width="700">
</p>

## How to install:
1. Clone or download the project
2. Import as existing project into the Eclipse IDE
3. Do the steps one and two for the [EcoreMetamodelExtraction project](https://github.com/tsaglam/EcoreMetamodelExtraction).
4. You need the Eclipse Modeling Framework, the Eclipse Java Development Tools, the Eclipse Plug-in Development Environment, the Xtend IDE and [XAnnotations](https://github.com/kit-sdq/XAnnotations). Make sure that all five are installed.
5. Run the project as Eclipse Application.
6. You can start the extraction from the context menu of a Java project (provisional UI).
