# JavaCodeEcorification
This is a proof of concept for the automatic integration of Ecore functionality into Java code. The Java code will be interlaced with the generated model code of an Ecore metamodel that was extracted from the original Java code with the help of the [EcoreMetamodelExtraction project](https://github.com/tsaglam/EcoreMetamodelExtraction).

## How to install:
1. Clone or download the project
2. Import as existing project into the Eclipse IDE
3. Do the steps one and two for the [EcoreMetamodelExtraction project](https://github.com/tsaglam/EcoreMetamodelExtraction).
4. You need the Eclipse Modeling Framework, the Eclipse Java Development Tools, the Eclipse Plug-in Development Environment, the Xtend IDE and [XAnnotations](https://github.com/kit-sdq/XAnnotations). Make sure that all five are installed.
5. Run the project as Eclipse Application.
6. You can start the extraction from the JCE menu in the menubar (provisional UI).