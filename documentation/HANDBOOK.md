## In-Depth AimSeg Usage Handbook

### Setup

AimSeg is conveniently distributed through a Fiji update site, simplifying the installation process and allowing users to effortlessly handle AimSeg updates. Comprehensive instructions for installing and configuring AimSeg are available [here](https://github.com/paucabar/AimSeg/blob/master/documentation/SETUP.md).

### Classifiers

AimSeg relies on both pixel and object classification, which must be completed prior to running the AimSeg core. Our documentation provides comprehensive [guidelines for training ilastik classifiers](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_TRAINING.md). Furthermore, the documentation includes instructions for utilising these ilastik classifiers to [process data](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_PROCESSING.md) and convert it into a compatible format for AimSeg.

Note that for enhanced generalisation, it's advisable to preprocess both the training data and the experimental data in a consistent manner. To facilitate this, AimSeg offers a [pre-processing command](https://github.com/paucabar/AimSeg/blob/master/documentation/HANDBOOK.md#pre-processing) that incorporates some common operations (e.g., normalisation).

### Getting Started

### Shortcuts

Visualisation shortcuts:

* <kbd>R</kbd> Toggle Show/Hide ROIs
* <kbd>F</kbd> Toggle ROI Fill/ROI stroke
* <kbd>N</kbd> Toggle Show/Hide RoiManager

Edition shortcuts:

* <kbd>G</kbd> Toggle Selected (RED)/Rejected (BLUE) ROI group
* <kbd>A</kbd> Add ROI
* <kbd>Shift</kbd><kbd>D</kbd> Delete ROI
* <kbd>L</kbd> Rename ROIs to image label
* <kbd>S</kbd> Split ROI
* <kbd>M</kbd> Merge ROIs
* <kbd>X</kbd> Get ROI convex hull
* <kbd>D</kbd> Dilate
* <kbd>E</kbd> Erode

Selection Tool shortcuts *(n = numerical keypad)*:

* <kbd>1</kbd> Select Brush Tool.
  * Shrink selected ROI by starting to draw from the outside
  * Expand Selected ROI by starting to draw from the inside
  * <kbd>U</kbd> Update ROI after manual editing
  * <kbd>n0</kbd> Select none (empty selection) before creating a new ROI with the Brush Tool
  * <kbd>n8</kbd> Increase the size of the Brush Tool
  * <kbd>n2</kbd> Decrease the size of the Brush Tool
* <kbd>2</kbd> Select Freehand Tool
* <kbd>3</kbd> Select Wand Tool

### Pre-processing

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik. Moreover, in order to reuse an ilastik project in different datasets, it will be necessary to pre-process the input data using the same parameters. Normalisation may be helpful to reuse the same trained model for different datasets.

1. Run the **Pre-processing** script (<code>Plugins>AimSeg>Pre-processing</code>)
2. Select the directory containing the image dataset to be pre-processed
3. Image width and height will be divided by the <code>Downsample</code> value
4. Check <code>Normalize</code> if you want to stretch the image histogram in a 0-1 range
5. Set the percentage of saturated pixels (<code>% Sat Pixels</code>)
6. Run

The pre-processed dataset will be stored in a new folder named after the selected folder adding the postfix  *_pre-processed*. The original images are converted to 32-bit for normalisation and saved as tif files.

### Parameterisation

[here](https://github.com/paucabar/AimSeg/blob/master/documentation/AimSeg%20demo%20-%20Main%20Menu.pdf).

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
