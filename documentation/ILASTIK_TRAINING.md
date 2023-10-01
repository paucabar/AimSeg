## Guide to Train Classifiers in ilastik

AimSeg necessitates both a pixel and an object classifier. In this guide, we present instructions for training classifiers in ilastik. For pixel classification, we recommend employing the autocontext classifier workflow, which involves performing pixel classification twice. The input to the second stage is constructed by appending the results of the first round as additional channels to the raw data. The output of the pixel/autocontext classifier serves as the input for the object classifier.

### Autocontext

Begin by creating a new project in <code>Autocontext</code>.

**Input Data**

* Access the <code>Raw Data</code> tab.
* Click on <code>Add new..</code> in the dropdown menu.
* Choose <code>Add separate Image(s)...</code>.
* Browse and select files from your image dataset.

**Feature Selection (1st Round)**

* Click on <code>Select Features...</code>.
* Opt for a subset of features or start by selecting all the features.
* Click <code>OK</code>.

**Training (1st Round)**

* Define 4 classes: myelin, axoplasm, membrane, and mitochondria.
* Annotate pixels for the different classes using the brush tool.
* Utilise the <code>Live Update</code> mode to enhance the annotation process, as it provides immediate feedback.

![image](https://github.com/paucabar/AimSeg/assets/39589980/f94d0f80-9319-4891-902a-b3c763828259)

**Feature Selection (2nd Round)**

* Click on <code>Select Features...</code>.
* Choose a subset of features or select all the features again.
* Click <code>OK</code>.

**Training (2nd Round)**

* Define 3 classes: myelin, axoplasm, and membrane (annotate mitochondria as axoplasm in this training round).
* Annotate pixels for the different classes using the brush tool.
* Leverage the <code>Live Update</code> mode for real-time feedback during the annotation process.

![image](https://github.com/paucabar/AimSeg/assets/39589980/a48c2ede-553a-4efd-90c0-97da9fc3d138)

**Prediction Export**

* From the <code>Source</code> drop-down menu, select <code>Probabilities Stage 2</code>.
* Click on <code>Choose Export Image Settings</code>.
* In the <code>Format</code> drop-down menu, opt for <code>tif</code> for faster data import performance in Fiji. Alternatively, you can select <code>hdf5</code>, but expect slower performance.
* **DO NOT** modify other settings.
* Click <code>OK</code>.

**Batch Processing**

* Click on <code>Select Raw Data Files...</code>.
* Browse and select the images to be processed.
* Click <code>Open</code>.
* Choose <code>Process all files</code>.
* Wait until the processing is complete.



### Object classification

To initiate a new project for Object Classification compatible with the Autocontext prediction, use the following format: <code>Object Classification [Inputs: Raw Data, Pixel Prediction Map]</code>.

**Input Data**

* Access the <code>Raw Data</code> tab and open the <code>Add new..</code> dropdown menu.
* Choose <code>Add separate Image(s)...</code>.
* Browse and select relevant files from your image dataset.
* Move to the <code>Prediction Maps</code> tab.
* Add the corresponding _Pixel Prediction Map_ for each _Raw Data_ image.
* Ensure that all _Raw Data_ files are correctly linked to their respective _Pixel Prediction Maps_.

**Threshold and Size Filter**

* In the <code>Method</code> dropdown menu, select <code>Simple</code>.
* Choose the axoplasm channel from the <code>Input</code> dropdown menu.
* Specify a sigma value in both <code>Smooth</code> boxes.
* Set a threshold to segment the axoplasm probability (within the range of 0-1).
* Define the minimum expected axon size (in pixels) in the <code>Min Size Filter</code> box.
* Specify the maximum expected axon size (in pixels) in the <code>Max Size Filter</code> box.

![Snag_26dad41f](https://user-images.githubusercontent.com/39589980/58541240-571fff80-81fb-11e9-9179-3d436db9009f.png)

**Object Feature Selection**

* Click on <code>Select Features</code>.
* Opt for <code>All excl. Location</code> to select all features except those related to the object's location.
* Click <code>OK</code>.

**Object Classification**

* Utilise <code>Add Label</code> to introduce new classes.
* You must have a minimum of 2 classes, such as axon or other.
* For our classifier, we employed 3 classes: axon (large), axon (small), and inner tongue.
* Label the white masks to train the classifier. Employ the brush tool to assign labels to masks, and use the eraser to remove labels.
* Activate <code>Live Update</code> to monitor the classifier's performance in real-time.

![OC steps](https://user-images.githubusercontent.com/39589980/58761085-56130900-8540-11e9-86c6-b23ee4917ea0.png)
