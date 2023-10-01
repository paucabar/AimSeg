## In-Depth AimSeg Usage Handbook

### Setup

AimSeg is conveniently distributed through a Fiji update site, simplifying the installation process and allowing users to effortlessly handle AimSeg updates. Comprehensive instructions for installing and configuring AimSeg are available [here](https://github.com/paucabar/AimSeg/blob/master/documentation/SETUP.md).

### Classifiers

AimSeg relies on both pixel and object classification, which must be completed prior to running the AimSeg core. Our documentation provides comprehensive [guidelines for training ilastik classifiers](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_TRAINING.md). Furthermore, the documentation includes instructions for utilising these ilastik classifiers to [process data](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_PROCESSING.md) and convert it into a compatible format for AimSeg.

Note that for enhanced generalisation, it's advisable to preprocess both the training data and the experimental data in a consistent manner. To facilitate this, AimSeg offers a [pre-processing command](https://github.com/paucabar/AimSeg/blob/master/documentation/HANDBOOK.md#pre-processing) that incorporates some common operations (e.g., normalisation).

### Getting Started

**Kickstart Tutorial**

The tutorial demonstrates how to run AimSeg and showcases various practical applications of the primary shortcuts in supervised mode.

<a href="https://www.youtube.com/watch?v=9TrODRqGJdQ"><img src="https://img.youtube.com/vi/9TrODRqGJdQ/maxresdefault.jpg" width="700"></a>

**Run AimSeg**

To initiate AimSeg within Fiji, navigate to <code>Plugins > AimSeg > AimSeg</code>. This action will bring up the Main Menu for AimSeg parameterisation. You can find detailed explanations for the setup options [here](https://github.com/paucabar/AimSeg/blob/master/documentation/AimSeg%20demo%20-%20Main%20Menu.pdf).

**Supervised Mode**

In supervised mode, users gain the capability to navigate through AimSeg's segmentation stages, enabling them to fine-tune the segmentation results as required.

*Stage 1: Inner Region*

* Upon opening, the selected image will appear, and detections will be displayed as two distinct ROI groups.
* The red ROIs correspond to the selected 'inner regions,' while the blue ROIs correspond to the rejected regions, representing the background.
* Users can edit the ROI selection using AimSeg's selection tools (please refer to the Shortcuts section below).
* Click "OK" to advance to the next stage.

*Stage 2: Fibre*

* The selections made in Stage 1 are superimposed as a magenta overlay on the electron microscopy image.
* Users can further edit the selection of fiber ROIs.
* Click "OK" to proceed to the next stage.

*Stage 3: Axon*

* The combination of the segmented inner regions and fiber instances allows for the determination of pixels corresponding to compact myelin, which is displayed as a magenta overlay on the electron microscopy image.
* Red ROIs correspond to the selected axons, while blue ROIs represent the rejected inner tongues.
* Users have the option to refine the ROI selection.
* Click "OK" to complete the annotation process.

**Shortcuts**

*Visualisation shortcuts:*

* <kbd>R</kbd> Toggle Show/Hide ROIs
* <kbd>F</kbd> Toggle ROI Fill/ROI stroke
* <kbd>N</kbd> Toggle Show/Hide RoiManager

*Edition shortcuts:*

* <kbd>G</kbd> Toggle Selected (RED)/Rejected (BLUE) ROI group
* <kbd>A</kbd> Add ROI
* <kbd>Shift</kbd><kbd>D</kbd> Delete ROI
* <kbd>L</kbd> Rename ROIs to image label
* <kbd>S</kbd> Split ROI
* <kbd>M</kbd> Merge ROIs
* <kbd>X</kbd> Get ROI convex hull
* <kbd>D</kbd> Dilate
* <kbd>E</kbd> Erode

*Selection Tool shortcuts (n = numerical keypad):*

* <kbd>1</kbd> Select Brush Tool.
  * Shrink selected ROI by starting to draw from the outside
  * Expand Selected ROI by starting to draw from the inside
  * <kbd>U</kbd> Update ROI after manual editing
  * <kbd>n0</kbd> Select none (empty selection) before creating a new ROI with the Brush Tool
  * <kbd>n8</kbd> Increase the size of the Brush Tool
  * <kbd>n2</kbd> Decrease the size of the Brush Tool
* <kbd>2</kbd> Select Freehand Tool
* <kbd>3</kbd> Select Wand Tool

**Results**

AimSeg generates exports in two distinct file formats:
* A result table (TSV) containing metrics for each fiber instance, including axon area, inner region area, fiber area, axon diameter, inner region diameter, fiber diameter, myelin g-ratio, and axon g-ratio. Additionally, it includes an edge label where 1 signifies a fiber on the image border, and 0 represents a complete fiber.
* Three sets of Fiji ROIs (ZIP files), each corresponding to the output of one of AimSeg's stages.

### Other AimSeg Commands

**Pre-processing**

Please note that ilastik only supports a series of file formats (check [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, it may be necessary to transform the image dataset into a format supported by ilastik. Moreover, in order to reuse an ilastik project in different datasets, it will be necessary to pre-process the input data using the same parameters. Normalisation may be helpful to reuse the same trained model for different datasets.

1. Run the **Pre-processing** script (<code>Plugins>AimSeg>Pre-processing</code>)
2. Select the directory containing the image dataset to be pre-processed
3. Image width and height will be divided by the <code>Downsample</code> value
4. Check <code>Normalize</code> if you want to stretch the image histogram in a 0-1 range
5. Set the percentage of saturated pixels (<code>% Sat Pixels</code>)
6. Run

The pre-processed dataset will be stored in a new folder named after the selected folder adding the postfix  *_pre-processed*. The original images are converted to 32-bit for normalisation and saved as tif files.


**Filtering (optional)**

It is possible to filter out those ROIs corresponding to fibre cross-sections on the edges or missing any ROI set. Additionally, if any ROI set is manually edited, the hierarchy will be re-established

1. Run the **Filtering** script (<code>Plugins>AimSeg>Post-processing RoiSets</code>)
2. <code>Browse</code> to select the _Image File_ (note that the RoiSets (zip files) must be stored in the image file parent folder to access them)
3. The <code>Axon Autocomplete</code> mode will create axon ROIs when these are missing, like the AimSeg workflow.
4. Check <code>Export Binary Mask</code> to store the corresponding ROI masks in the working directory.
5. <code>Run</code>

## How to cite

Ana Maria Rondelli, Jose Manuel Morante-Redolat, Peter Bankhead, Bertrand Vernay, Anna Williams, Pau Carrillo-Barberà. AimSeg: a machine-learning-aided tool for axon, inner tongue and myelin segmentation. bioRxiv 2023.01.02.522533; doi: https://doi.org/10.1101/2023.01.02.522533
