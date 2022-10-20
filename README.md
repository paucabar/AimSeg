# AimSeg - Axon, Inner tongue and Myelin Segmentation

## Table of contents

* [Overview](https://github.com/paucabar/AimSeg/blob/master/README.md#overview)
* [How to cite](https://github.com/paucabar/AimSeg/blob/master/README.md#how-to-cite)
* [Requirements](https://github.com/paucabar/AimSeg/blob/master/README.md#requirements)
* [Installation](https://github.com/paucabar/AimSeg/blob/master/README.md#installation)
* [Test Dataset](https://github.com/paucabar/AimSeg/blob/master/README.md#test-dataset)
* [Usage](https://github.com/paucabar/AimSeg/blob/master/README.md#usage)
   * [Pre-processing (Fiji)](https://github.com/paucabar/AimSeg/blob/master/README.md#pre-processing-fiji)
   * [Pixel classification (ilastik)](https://github.com/paucabar/AimSeg/blob/master/README.md#pixel-classification-ilastik)
   * [Object classification (ilastik)](https://github.com/paucabar/AimSeg/blob/master/README.md#object-classification-ilastik)
   * [AimSeg (Fiji)](https://github.com/paucabar/AimSeg/blob/master/README.md#aimseg-fiji)
   * [Filtering (optional)](https://github.com/paucabar/AimSeg/blob/master/README.md#filtering-optional)
* [Train Classifiers](https://github.com/paucabar/AimSeg/blob/master/README.md#train-classifiers)

## Overview

AimSeg is a bioimage analysis workflow that combines machines learning with automated post-processing and user-guidance to perform the segmentation of axon, inner cytoplasmic tongue and compact myelin on electron microscopy (EM) data. The workflow uses pixel and object classification to facilitate the process, whereas a semi-automated mode enables users to amend the automated selection performed by AimSeg.

## How to cite

Available soon...

## Requirements

* [Fiji](https://fiji.sc/)
* [ilastik](https://www.ilastik.org/) 1.3.3 or later
* _ilastik_ update site (Fiji). Once added, it is important to set up the connection between ilastik and Fiji (only needs to be done once). Please fins a short guide below
* _Morphology_ update site (Fiji)

---
**How to follow an update site in Fiji**

See [here](https://imagej.net/Following_an_update_site)

**How to set up the connection between ilastik and Fiji**

* In Fiji, click on <code>Plugins > ilastik > Configure ilastik executable location</code>
* <code>Browse</code> to select the *Path to ilastik executable*
    * E.g., the typical Windows path is <code>C:\Program Files\ilastik-1.3.3post3\ilastik.exe</code> (mind the ilastik version if you are using a most recent one: <code>C:\Program Files\ilastik-[version]\ilastik.exe</code>)

![image](https://user-images.githubusercontent.com/39589980/187649952-dfea6302-d439-49a9-9e2a-d00520ecc0b2.png)

---

## Installation

1. Start Fiji
2. Start the **ImageJ Updater** (<code>Help > Update...</code>)
3. Click on <code>Manage update sites</code>
4. Click on <code>Add update site</code>
5. A new blank row is to be created at the bottom of the update sites list
6. Type **AimSeg** in the **Name** column
7. Type **http://sites.imagej.net/AimSeg/** in the **URL** column
8. <code>Close</code> the update sites window
9. <code>Apply changes</code>
10. Restart Fiji
11. Check if <code>AimSeg</code> appears now in the <code>Plugins</code> dropdown menu (note that it will be placed at the bottom of the dropdown menu)

## Test Dataset

Download an example [image dataset](https://drive.google.com/drive/folders/1DEFtn71krM6cOjsnZpIEFMCBVmAYk9Lq?usp=sharing) and two [ilastik projects](https://drive.google.com/drive/folders/1tNyDpmd0wwBx-MKH-LhOZaZBgYMfTz2J?usp=sharing) for pixel and object classification.

* To use the provided image dataset and ilastik classifiers, just follow the documentation below and always use the defaults for parameterisation.
* To train your own classifiers, please go to [Train Classifiers](https://github.com/paucabar/AimSeg/blob/master/README.md#train-classifiers).

## Usage

### Pre-processing (Fiji)

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik. Moreover, in order to reuse an ilastik project in different datasets, it will be necessary to use the same settings in the format transformation step to pre-process the input data.

1. Run the **Pre-processing** script (<code>Plugins>AimSeg>Pre-processing</code>)
2. Select the directory containing the image dataset to be transformed
3. Check <code>Normalize</code> if you want to use the Fiji's Enhance Contrast command (0.3 % sat pixels)
4. Run

The pre-processed dataset will be stored in a new folder named after the selected folder adding the postfix  *_pre-processed*. The original images are converted to 8-bit and saved as tif files.

### Pixel classification (ilastik)

1. Click on <code>Browse Files</code> (Open Project... submenu)
2. Select a pre-trained pixel classifier
3. Go to the <code>Batch Processing</code> applet
4. Click on <code>Select Raw Data Files...</code>
5. Load the raw data files
6. Click on <code>Process all files</code>

![pc1](https://user-images.githubusercontent.com/39589980/186393550-d30c133c-d275-4104-a467-8bb1d56910f1.png)

By default, ilastik will store the output files together with the input files. Please do not relocate these files, as AimSeg needs the ilastik output files to be stored in the same folder, henceforth the working directory.

### Object classification (ilastik)

1. Click on <code>Browse Files</code> (Open Project... submenu)
2. Select a pre-trained object classifier
3. Go to the <code>Batch Processing</code> applet
4. Click on <code>Select Raw Data Files...</code>
5. Load the raw data files
6. Switch to the <code>Prediction Maps</code> tab
7. Click on <code>Select Prediction Maps Files...</code>
8. Load the prediction maps files (sorted to match the raw data list)
9. Click on <code>Process all files</code>

![pc2](https://user-images.githubusercontent.com/39589980/186394567-8c6c5a3c-80f0-4e1b-b810-071d8183ecbb.png)

By default, ilastik will store the output files together with the input files. Please do not relocate these files, as AimSeg needs the ilastik output files to be stored in the working directory.

### AimSeg (Fiji)

*Run AimSeg*

1. Run the **AimSeg** script (<code>Plugins>AimSeg>AimSeg</code>)
2. Select the electron microscopy image to be procecessed (please remember that the image dataset and the ilastik output, both probability maps and object predictions, must remain stored together in the working directory)
3. Set the myelin probability channel and the threshold to segment the object prediction image
4. The <code>Automated</code> mode will skip the user-edition steps
5. The <code>Axon Autocomplete</code> will create axon ROIs when these are not detected by AimSeg or added by the user. In this case, AimSeg assumes that the inner tongue of those fibres missing the axon ROI has shrunk completely, so it automatically generates an axon ROI equivalent to the inner compact myelin layer (ICML) ROI.
6. AimSeg may take a few seconds to initialise, as it starts importing the image files.

*Stage 1: Inner compact myelin layer (ICML)*

* The elecron microscopy image will pop up and the objects will be detected as ROIs. The red ROIs correspond to the selected objects, whereas the blue ROIs corresponds to the rejected objects
* It is possible to edit the ROI selection using the Fiji selection tools (please see the **ROI edition** section below)
* Click ok to proceed to the next stage

*Stage 2: Fibre*

* The Stage 1 selection is added as a magenta overlay to the elecron microscopy image
* It is possible to edit the ROI selection
* Click ok to proceed to the next stage

*Stage 3: Axon*

* The selected compact myelin is added as a magenta overlay to the elecron microscopy image. The red ROIs correspond to the selected objects, whereas the blue ROIs corresponds to the rejected objects
* It is possible to edit the ROI selection
* Click ok to finish the annotation process

*Post-processing and quantification*

The last steps are fully automated. A results table file will be stored in the working directory.

---
**ROI edition**

It is possible to edit the ROIs using the Fiji selection tools during the 3 AimSeg stages. Additionally, stage 1 and 3 generate blue ROIs correponding to rejected objects. The user can toggle ROIs between selected (red) and rejected (blue). Below are listed the AimSeg shortcuts for ROI edition:

* <kbd>q</kbd> Toggle ROI mode (red/blue)
* <kbd>a</kbd> Add ROI (note that those ROIs added using the Fiji default <kbd>t</kbd> shortcut will be ignored by AimSeg)
* <kbd>d</kbd> Delete ROI
* <kbd>u</kbd> Update ROI (edit the ROI before updating. E.g., hold <kbd>Shift</kbd> key while drawing a region to be added to the selected ROI. Conversely, hold <kbd>Alt</kbd> key while drawing a hole or region to be removed from the selected ROI)
* <kbd>z</kbd> Split ROI (will split a ROI made of multiple selection into different ROIs)
* <kbd>c</kbd> Get convex hull
* <kbd>0</kbd> Select none (empty selection)

AimSeg shortcuts to set the selection tool:

* <kbd>1</kbd> Select Brush Tool. Note that once created a brush selection, it will be only possible to edit that selection. To addd a new selection, first deselect the latest one by pressing <kbd>0</kbd> (select none)
* <kbd>2</kbd> Select Freehand Tool
* <kbd>3</kbd> Select Wand Tool
---

### Filtering (optional)

It is possible to filter out those ROIs corresponding to fibre cross-sections on the edges or missing any ROI set. Currently, this option is not included in AimSeg, but it can be done after running the main AimSeg workflow:

1. In this GitHub repository, go to the [Post-processing_RoiSets](https://github.com/paucabar/AimSeg/blob/master/Post-processing_RoiSets.groovy) script
2. Copy the code
3. In Fiji, create a new script (<code>File > > New > Script...</code>)
4. In the script editor, set Groovy as the script language (<code>Language > Groovy</code>)
5. <code>Run</code>
6. <code>Browse</code> to select the _Image File_ (note that the RoiSets (zip files) must be stored in the image file parent folder to access them)
7. The <code>Axon Autocomplete</code> will create axon ROIs when these are missing, like the AimSeg workflow.

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
