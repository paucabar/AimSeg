# Axon AIM

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
6. Type **NeuroMol Lab** in the **Name** column
7. Type **http://sites.imagej.net/Paucabar/** in the **URL** column
8. <code>Close</code> the update sites window
9. <code>Apply changes</code>
10. Restart Fiji
11. Check if <code>NeuroMol Lab</code> appears now in the <code>Plugins</code> dropdown menu (note that it will be placed at the bottom of the dropdown menu)

## Test Dataset

Download an example [image dataset](https://drive.google.com/drive/folders/1DEFtn71krM6cOjsnZpIEFMCBVmAYk9Lq?usp=sharing) and two [ilastik projects](https://drive.google.com/drive/folders/1tNyDpmd0wwBx-MKH-LhOZaZBgYMfTz2J?usp=sharing) for pixel and object classification.

## Usage

### Format Transformation

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik.

1. Run the **Format transformation** macro (Plugins>NeuroMol Lab>G-ratio>Format Transformation)
2. Select the directory containing the image dataset to be transformed
3. It is possible to convert the images into 8-bit or to normalize the data
4. It is possible to export the images as TIF or HDF5 files
5. Type the extension of the files to be transformed. Files with a different extension will be ignored
6. Type a postfix (a new folder will be named after the original folder adding the postfix separated by an underscore)
7. Run

* In order to use the provided image dataset and ilastik classifiers, normalize the images and save them as TIF files

### Pixel classification

### Object classification

## G-Ratio Aide

## Results Table
