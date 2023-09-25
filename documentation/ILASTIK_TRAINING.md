## Train Classifiers

### Pixel classification (ilastik)

1. Create a new <code>Pixel Classification</code> project
2. Set the workflow modules:

**Input Data**

* Click on the <code>Add new..</code> dropdown menu located in the <code>Raw Data</code> tab
* Click on <code>Add separate Image(s)...</code>
* Browse and select some files from your image dataset

**Feature Selection**

* Click on <code>Select Features...</code>
* Select a subset of features. Alternatively, start by selecting all the features.
* Click _<code>OK</code>

**Training**

* Create 3 classes: myelin, axoplasm and membrane
* Annotate pixels for the different classes using the brush tool
* The <code>Live Update</code> mode will facilitate the annotation process, as it provides the user with immediate feedback

![pixel classification annotation](https://user-images.githubusercontent.com/39589980/197034819-69fb2d5e-d27f-4548-af0b-9e5d0f4ce3f0.png)

**Prediction Export**

* In the <code>Source</code> drop-down menu select <code>Probabilities</code>
* Click on <code>Choose Export Image Settings</code>
* In the <code>Format</code> drop-down menu, select <code>tif</code> for a faster performance of data import in Fiji. Alternatively, <code>hdf5</code> can also be imported, but expect a slower performance
* **DO NOT** change the other settings
* Click <code>OK</code>

**Batch Processing**

* Click on <code>Select Raw Data Files...</code>
* Browse the entire image dataset
* Click on <code>Open</code>
* Click on <code>Process all files</code>
* Wait until the process is done

### Object classification (ilastik)

1. Create a new <code>Object Classification [Inputs: Raw Data, Pixel Prediction Map]</code> project
2. Set the workflow modules

**Input Data**

* Click on the <code>Add new..</code> dropdown menu located in the <code>Raw Data</code> tab
* Click on <code>Add separate Image(s)...</code>
* Browse and select some files from your image dataset
* Select the <code>Prediction Maps</code> tab
* Add the _Pixel Prediction Map_ corresponding to each Raw Data
* Check that all the Raw Data files are linked properly to its corresponding _Pixel Prediction Map_

**Threshold and Size Filter**

* Select <code>Simple</code> in the <code>Method</code> dropdown menu
* Select the axoplasm channel in the <code>Input</code> dropdown menu
* Set a sigma value in the two <code>Smooth</code> boxes
* Set a threshold to segment the axoplasm probability (range 0-1)
* Set the minimun size of the expected axons (in pixels) in the <code>Min Size Filter</code> box
* Set the maximum size of the expected axons (in pixels) in the <code>Max Size Filter</code> box

![Snag_26dad41f](https://user-images.githubusercontent.com/39589980/58541240-571fff80-81fb-11e9-9179-3d436db9009f.png)

**Object Feature Selection**

* Click on <code>Select Features</code>
* Click on <code>All excl. Location</code> to select all the features, except those related with the object location
* Click <code>OK</code>

**Object Classification**

* Click on <code>Add Label</code> to add new classes
* The minimum number of classes is 2: axon or other.
* We used 3 for our classifier: axon (big), axon (small), inner tongue
* Label the white masks  to train the classifier. Use the brush to assign labels to masks and the eraser to eliminate labels
* Click on <code>Live Update</code> to check how the classifier is performing

![OC steps](https://user-images.githubusercontent.com/39589980/58761085-56130900-8540-11e9-86c6-b23ee4917ea0.png)
