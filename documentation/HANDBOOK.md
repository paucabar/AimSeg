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
