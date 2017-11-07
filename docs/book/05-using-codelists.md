# Using codelists

Codelists can be used to resolve coded values to human readable ones. 

#### Adding a codelist

To add a codelists, choose ```Codelists``` in the menu and click on the plus sign at the top.

![ldproxy Manager - add codelist](../img/codelist-01.png)

A dialog will appear where you can enter a codelist identifier and the URL of the codelists. For now only codelists encoded as GML dictionary are supported.

![ldproxy Manager - new codelist](../img/codelist-02.png)

We will use a codelist from Germany as an example:

- URL: [https://services.interactive-instruments.de/aaa/cl/AX_Bahnkategorie/index.gml](https://services.interactive-instruments.de/aaa/cl/AX_Bahnkategorie/index.gml)

![ldproxy Manager - add bahnkategorie](../img/codelist-03.png)

When you press `Add`, ldproxy will import the codelist. 

![ldproxy Manager - added bahnkategorie](../img/codelist-04.png)

#### Using a codelist

Now you can open the feature type configuration where you want to resolve a coded value and select your codelist on the left.

![ldproxy Manager - using bahnkategorie](../img/codelist-05.png)

