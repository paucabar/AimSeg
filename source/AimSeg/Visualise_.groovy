#@ File(label="Image File", style="open") imageFile

import java.io.File
import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import org.ilastik.ilastik4ij.hdf5.Hdf5
import static org.ilastik.ilastik4ij.util.ImgUtils.reversed
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes
import ij.plugin.frame.RoiManager
import ij.gui.Overlay
import ij.plugin.filter.ThresholdToSelection
import ij.process.ImageProcessor
import ij.gui.Roi
import java.awt.Color


/**
 * Function to import an image from an HDF5 file with ilastik4ij
 */
ImagePlus importHDF5Image(File inputFile, String datasetName, String axisOrder) {
	def imp = Hdf5.readDataset(inputFile, datasetName, toImagejAxes(reversed(axisOrder.toLowerCase())))
	ImagePlus result = ImageJFunctions.wrap(imp, "Some title here")
	return result
}

/**
 * Opens an image file
 * If the file extension is h5, the image is imported using the ilastik's importer
 * Otherwise, the file is imported using ImageJ's opener
 */
ImagePlus importImage (File inputFile, String datasetName, String axisOrder) {
    String imagePath = inputFile.getAbsolutePath()
    if (!imagePath.endsWith(".h5")) {
        def opener = new Opener()
        String extension = imagePath[imagePath.lastIndexOf('.')+1..-1]
        println "Importing $extension file"
        result = opener.openUsingBioFormats(imagePath)
    } else {
        println "Importing h5 file"
        result = importHDF5Image(inputFile, datasetName, axisOrder)
	}
    return result
}

ImagePlus label_semantic (ImagePlus impLabel, int classIndex, RoiManager roiSet) {
	ip = impLabel.getProcessor()
	roiSet.getRoisAsArray().each { roi ->
		ip.setColor(classIndex)
		ip.fill(roi)
	}	
	ip.setMinAndMax(0.0, 4.0)
	ImagePlus imp_semantic = new ImagePlus("labels", ip)
	return imp_semantic
}

ImagePlus setMaskOverlay (ImagePlus imp, ImagePlus impMask, int label, int R, int G, int B, int alpha) {
    ImageProcessor ip = impMask.getProcessor()
    ip.setThreshold (label, label)
    def tts = new ThresholdToSelection()
    Roi roi = tts.convert(ip)

    def ovl = new Overlay(roi)
    ovl.setFillColor(new Color(R,G,B,alpha))

    imp.setOverlay(ovl)
    ImagePlus impFlatten = imp.flatten()

    ovl.clear()
    imp.setOverlay(ovl)

    return impFlatten
}

// import EM image
imp = importImage(imageFile, "/data", "tzyxc")

// create 3 RoiManager
RoiManager rmIn = new RoiManager(false)
RoiManager rmOut = new RoiManager(false)
RoiManager rmAxon = new RoiManager(false)

String parentPathS = imageFile.getParentFile()
String impNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))
rmIn.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
rmOut.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")
rmAxon.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")

// create an image to add labels
ImagePlus impLabel = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)

// add labels
impLabel = label_semantic (impLabel, 1, rmOut)
impLabel = label_semantic (impLabel, 2, rmIn)
impLabel = label_semantic (impLabel, 3, rmAxon)

// set label colours
impMyelin = setMaskOverlay (imp, impLabel, 1, 82, 207, 255, 128)
impTongue = setMaskOverlay (impMyelin, impLabel, 2, 255, 165, 4, 128)
impAxon = setMaskOverlay (impTongue, impLabel, 3, 160, 205, 0, 128)
