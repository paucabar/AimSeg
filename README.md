# AimSeg - Axon, Inner tongue and Myelin Segmentation

## Overview

AimSeg is a bioimage analysis tool that blends machine learning, automated post-processing, and user guidance to achieve the segmentation of axons, inner tongue, and compact myelin in electron microscopy data. The workflow relies on pixel and object classifiers trained in ilastik, while a supervised mode empowers users to refine the automated selections made by AimSeg.

## Kickstart Tutorial



## How to cite

If you use AimSeg for your research, please cite:
* Ana Maria Rondelli, Jose Manuel Morante-Redolat, Peter Bankhead, Bertrand Vernay, Anna Williams, Pau Carrillo-Barberà. AimSeg: a machine-learning-aided tool for axon, inner tongue and myelin segmentation. bioRxiv 2023.01.02.522533; doi: https://doi.org/10.1101/2023.01.02.522533

## Dataset and pre-trained classifiers
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.8351731.svg)](https://doi.org/10.5281/zenodo.8351731)

The AimSeg ground truth and classifiers are available in [Zenodo](https://doi.org/10.5281/zenodo.8351731).

## Usage

The AimSeg core is run in Fiji, but it also requires a pixel and an object classifier. Our workflow uses ilastik, but AimSeg has the potential to use probability maps and object predictions generated using different tools. The documentation includes instructions to use models pre-trained in ilastik. The subtitle of each section includes, in parentheses, the software to be used (i.e., Fiji or ilastik).

NOTE: to use the example dataset and pre-trained models just use the AimSeg default parameters.

### Pre-processing (Fiji)

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik. Moreover, in order to reuse an ilastik project in different datasets, it will be necessary to pre-process the input data using the same parameters. Normalisation may be helpful to reuse the same trained model for different datasets.

1. Run the **Pre-processing** script (<code>Plugins>AimSeg>Pre-processing</code>)
2. Select the directory containing the image dataset to be pre-processed
3. Image width and height will be divided by the <code>Downsample</code> value
4. Check <code>Normalize</code> if you want to stretch the image histogram in a 0-1 range
5. Set the percentage of saturated pixels (<code>% Sat Pixels</code>)
6. Run

The pre-processed dataset will be stored in a new folder named after the selected folder adding the postfix  *_pre-processed*. The original images are converted to 32-bit for normalisation and saved as tif files.

### AimSeg (Fiji)

**AimSeg parameters**

1. Run **AimSeg** (<code>Plugins>AimSeg>AimSeg</code>)
2. Select the electron microscopy image (one at a time) to be procecessed (please remember that the image dataset and the ilastik output, both probability maps and object predictions, must remain stored together in the working directory)
3. Set the <code>Myelin Probability Channel</code> (the ilastik pixel classification generates a probability map, i.e., a multi-channel image where each channel corresponds to the different pixel labels defined for the training; starts from 1)
4. Set the <code>Myelin threshold</code> to find the myelinated axons
5. Set the <code>Min Area</code> of the inner region (wrapped by compact myelin)
6. Set the <code>Min Circularity</code> of the inner region
7. Set a value to <code>correct the myelin threhold</code> to define the fibre outline
8. Set the parameters to slect the objects classified as axons (the ilastik object classification generates a object prediction, i.e., a single image where the pixel values of each object correspond to the different object labels defined for the training; starts from 1). Set the <code>Object Prediction Threshold</code> and specify if the selected objects are expected to be <code>Below</code> or <code>Above</code> the threshold.
9. AIimSeg includes some methods to further process the axon mask, which are listed under the <code>Axon Correction</code> menu. Methods based on watershed need to specify the <code>Axon Probability Channel</code>
10. Set the <code>Automated</code> mode to skip the user-edition steps. Otherwise AimSeg will enter the <code>Supervised</code>
11. The <code>Axon Autocomplete</code> will create axon ROIs when these are not detected by AimSeg or added by the user. In this case, AimSeg assumes that the inner tongue of those fibres missing the axon ROI has shrunk completely, so it automatically generates an axon ROI equivalent to the inner region ROI. Recommended only for <code>Supervised</code> mode

**Supervised Mode**

*Stage 1: Inner Region*

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


### Filtering (optional)

It is possible to filter out those ROIs corresponding to fibre cross-sections on the edges or missing any ROI set. Additionally, if any ROI set is manually edited, the hierarchy will be re-established

1. Run the **Filtering** script (<code>Plugins>AimSeg>Post-processing RoiSets</code>)
2. <code>Browse</code> to select the _Image File_ (note that the RoiSets (zip files) must be stored in the image file parent folder to access them)
3. The <code>Axon Autocomplete</code> mode will create axon ROIs when these are missing, like the AimSeg workflow.
4. Check <code>Export Binary Mask</code> to store the corresponding ROI masks in the working directory.
5. <code>Run</code>

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
