# AimSeg

## Requirements

* [Fiji](https://fiji.sc/)
* [ilastik](https://www.ilastik.org/) 1.3.3
* _ilastik_ update site (Fiji). Once added, it is important to configure the ilastik executable location within the Fiji's plugin.
* _Morphology_ update site (Fiji)

How to [follow an update site](https://imagej.net/Following_an_update_site) in Fiji

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

* In order to use the provided image dataset and ilastik classifiers, use the **Format transformation** macro (see below) to convert the images into 8-bit and save them as TIF files.

## Usage

### Format Transformation

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik. Moreover, in order to reuse an ilastik project in different datasets, it will be necessary to use the same settings in the format transformation step to pre-process the input data.

1. Run the **Format transformation** macro (Plugins>AimSeg>Format Transformation)
2. Select the directory containing the image dataset to be transformed
3. It is possible to convert the images into 8-bit or to normalize the data
4. It is possible to export the images as TIF or HDF5 files
5. Type the extension of the files to be transformed. Files with a different extension will be ignored
6. Type a postfix (a new folder will be named after the original folder adding the postfix separated by an underscore)
7. Run

### Pixel classification

1. Load a pre-trained pixel classifier in ilastik
2. Go to the Batch Processing applet
3. Click on _Select Raw Data Files..._
4. Load the raw data files
5. Click on _Process all files_

### Object classification

1. Load a pre-trained object classifier in ilastik
2. Go to the Batch Processing applet
3. Click on _Select Raw Data Files..._
4. Load the raw data files
5. Switch to the _Prediction Maps_ tab
6. Click on _Select Prediction Maps Files..._
7. Load the prediction maps files (sorted to match the raw data list)
8. Click on _Process all files_

### G-Ratio Aide

1. Run the **G-ratio Aide** macro (Plugins>AimSeg>G-Ratio Aide)
2. Select the directory containing the image dataset and the ilastik output (both probability maps and object predictions)
3. Select an image to be annotated
4. The pre-processing step may take a few seconds

*Stage 1: Myelin inner boundary*

* The macro will display the raw elecron microscopy image merged with two additional channels. The red channel corresponds to the selected objects, whereas the blue channel corresponds to the rejected objects
* It is possible to edit the ROI selection using the Fiji selection tools. Additionally, the wand tool can be used to select blue, rejected regions with a single click
* Click ok to proceed to the next stage

*Stage 2: Fibre*

* The macro will display the raw elecron microscopy image merged with an additional magenta channel corresponding to the Stage 1 selection
* It is possible to edit the ROI selection using the Fiji selection tools
* Click ok to proceed to the next stage

*Stage 3: Axoplasm*

* The macro will display the raw elecron microscopy image merged with two additional channels. The magenta channel corresponds to the selected myelin (Stage 1 XOR Stage 2), whereas the blue channel corresponds to the potencial axoplasm objects rejected by the classifier (i.e., labeled as inner tongue)
* It is possible to edit the ROI selection using the Fiji selection tools. Additionally, the wand tool can be used to select blue, rejected regions with a single click
* Click ok to finish the annotation process

### Results Table

1. Run the **Results Table** macro (Plugins>AimSeg>Results Table)
2. Select the directory containing the image dataset, the ilastik output and the ROI files 
3. The macro will automatically analyze all the complete ROI sets 
