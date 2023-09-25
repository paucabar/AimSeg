## Guide to Processing Files in ilastik Using Pre-trained Classifiers

**Pixel Classification or Autocontext Project**

1. Click on <code>Browse Files</code> (found in the Open Project submenu).
2. Select a pre-trained pixel classifier or autocontext project.
3. Navigate to the <code>Batch Processing</code> applet.
4. Click on <code>Select Raw Data Files...</code>.
5. Choose and load all the images that need prediction.
6. Click on <code>Process all files</code>. The processing time may vary based on factors like the number of images, image sizes, or the complexity of classifier features.

![pc1](https://user-images.githubusercontent.com/39589980/186393550-d30c133c-d275-4104-a467-8bb1d56910f1.png)

By default, ilastik stores the output files alongside the input files. Please avoid moving these files, as AimSeg requires ilastik output files to be in the same folder, henceforth the working directory.

**Object Classification Project**

If you've used the pixel classifier before, click <code>Project > Close</code> to return to the ilastik main menu. Do not save changes unless you've modified the training or export parameters (e.g., used different features, added new annotations, or selected a different file type for export) that you want to keep.

1. Click on <code>Browse Files</code> (found in the Open Project submenu).
2. Select a pre-trained object classifier.
3. Go to the <code>Batch Processing</code> applet.
4. Click on <code>Select Raw Data Files...</code>.
5. Choose and load all the images that need prediction.
6. Switch to the <code>Prediction Maps</code> tab.
7. Click on <code>Select Prediction Maps Files...</code>.
8. Load the prediction map files (ensure they are sorted to match the raw data list).
9. Click on <code>Process all files</code>.

![pc2](https://user-images.githubusercontent.com/39589980/186394567-8c6c5a3c-80f0-4e1b-b810-071d8183ecbb.png)

By default, ilastik stores the output files alongside the input files. Please avoid moving these files, as AimSeg requires ilastik output files to be in the working directory.
