## In-Depth AimSeg Usage Handbook

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
