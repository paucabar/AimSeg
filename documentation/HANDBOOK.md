## In-Depth AimSeg Usage Handbook

### Setup

AimSeg is conveniently distributed through a Fiji update site, simplifying the installation process and allowing users to effortlessly handle AimSeg updates. Comprehensive instructions for installing and configuring AimSeg are available [here](https://github.com/paucabar/AimSeg/blob/master/documentation/SETUP.md).

### Classifiers

AimSeg relies on both pixel and object classification, which must be completed prior to running the AimSeg core. Our documentation provides comprehensive [guidelines for training ilastik classifiers](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_TRAINING.md). Furthermore, the documentation includes instructions for utilising these ilastik classifiers to [process data](https://github.com/paucabar/AimSeg/blob/master/documentation/ILASTIK_PROCESSING.md) and convert it into a compatible format for AimSeg.

Note that for enhanced generalisation, it's advisable to preprocess both the training data and the experimental data in a consistent manner. To facilitate this, AimSeg offers a [pre-processing command](https://github.com/paucabar/AimSeg/blob/master/documentation/HANDBOOK.md#other-aimseg-commands) that incorporates some common operations (e.g., normalisation).

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

Please take note that ilastik has specific supported file format (refer to [Supported File Formats](https://www.ilastik.org/documentation/basics/dataselection.html)). Therefore, you may need to convert your image dataset into a format compatible with ilastik. Additionally, to utilise the same trained model on different datasets, it's important to pre-process the input data with consistent parameters. Normalisation can be advantageous when aiming to apply the same trained model to various datasets.

1. Execute the **Pre-processing** script by going to <code>Plugins > AimSeg > Pre-processing</code>.
2. Select the directory containing the image dataset that requires pre-processing.
3. The image width and height will be divided by the <code>Downsample</code> value.
4. Check the <code>Normalize</code> option if you wish to stretch the image histogram within a 0-1 range.
5. Set the percentage of saturated pixels using <code>% Sat Pixels</code>.
6. Click the <code>Run</code> button.

The pre-processed dataset will be saved in a new folder named after the selected folder with the addition of the postfix __pre-processed_. The original images are converted to 32-bit for normalisation and saved as TIF files.

**Post-processing RoiSets (optional)**

This command allows you to filter out ROIs corresponding to fiber cross-sections on the edges or any missing ROI sets. Additionally, if any ROI set is manually edited, the hierarchy will be re-established.

1. Run the **Post-processing** RoiSets script by navigating to <code>Plugins > AimSeg > Post-processing RoiSets</code>.
2. Use the <code>Browse</code> button to select the _Image File_ (note that the RoiSets (ZIP) should be stored in the parent folder of the image file for access).
3. The <code>Axon Autocomplete</code> mode will generate axon ROIs in cases where they are missing, following the AimSeg workflow.
4. Check the <code>Export Binary Mask</code> option to store the corresponding ROI masks in the working directory.
5. Click <code>Run</code>.

**Export Masks**

This command exports two different types of masks:
* Instance labels for the axon, inner region, and fiber.
* Semantic masks obtained by combining the labels, differentiating between four semantic classes: axon, inner tongue, myelin, and background.

**Visialise**

This command displays an overlay of the semantic segmentation on the original image for visualization purposes.

## How to cite

Ana Maria Rondelli, Jose Manuel Morante-Redolat, Peter Bankhead, Bertrand Vernay, Anna Williams, Pau Carrillo-Barberà. AimSeg: a machine-learning-aided tool for axon, inner tongue and myelin segmentation. bioRxiv 2023.01.02.522533; doi: https://doi.org/10.1101/2023.01.02.522533
