#@ File(label="Image File", style="open") imageFile

import java.io.File
import ij.IJ
import ij.io.Opener
import ij.io.FileSaver
import ij.ImagePlus
import org.ilastik.ilastik4ij.hdf5.Hdf5
import static org.ilastik.ilastik4ij.util.ImgUtils.reversed
import static org.ilastik.ilastik4ij.util.ImgUtils.toImagejAxes
import ij.plugin.frame.RoiManager
import ij.plugin.filter.ThresholdToSelection
import ij.process.ImageProcessor
import ij.gui.Roi


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

ImagePlus label_semantic(ImagePlus impSemantic, int classIndex, RoiManager roiSet) {
	ip = impSemantic.getProcessor()
	roiSet.getRoisAsArray().each { roi ->
		ip.setColor(classIndex)
		ip.fill(roi)
	}	
	ip.setMinAndMax(0.0, impSemantic.getStatistics().max)
	impSemantic = new ImagePlus("Semantic Labels", ip)
	IJ.run(impSemantic, "glasbey on dark", "")
	return impSemantic
}

ImagePlus label_instance(ImagePlus impInstance, RoiManager roiSet, String objectName) {
	ip = impInstance.getProcessor()
	roiSet.getRoisAsArray().each { roi ->
		ip.setColor(roi.getName() as int)
		ip.fill(roi)
	}	
	ip.setMinAndMax(0.0, impInstance.getStatistics().max)
	impInstance = new ImagePlus("$objectName Labels", ip)
	IJ.run(impInstance, "glasbey on dark", "")
	return impInstance
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

// create images to add labels
ImagePlus impSemantic = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
ImagePlus impFibre = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
ImagePlus impInnerMyelin = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
ImagePlus impAxon = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)

// draw semantic labels
impSemantic = label_semantic (impSemantic, 1, rmOut)
impSemantic = label_semantic (impSemantic, 2, rmIn)
impSemantic = label_semantic (impSemantic, 3, rmAxon)

// draw instances
impFibre = label_instance(impFibre, rmOut, "Fibre")
impInnerMyelin = label_instance(impInnerMyelin, rmIn, "Inner Myelin")
impAxon = label_instance(impAxon, rmAxon, "Axon")

// create directories
File exportDir = new File(parentPathS+File.separator+"export")
File semanticDir = new File(exportDir.getAbsolutePath()+File.separator+"semantic")
File instanceDir = new File(exportDir.getAbsolutePath()+File.separator+"instance")
File fibreDir = new File(instanceDir.getAbsolutePath()+File.separator+"fibre")
File innerMyelinDir = new File(instanceDir.getAbsolutePath()+File.separator+"inner_myelin")
File axonDir = new File(instanceDir.getAbsolutePath()+File.separator+"axon")
exportDir.mkdir()
semanticDir.mkdir()
instanceDir.mkdir()
fibreDir.mkdir()
innerMyelinDir.mkdir()
axonDir.mkdir()

// save images
new FileSaver(impSemantic).saveAsTiff(semanticDir.getAbsolutePath()+File.separator+impNameWithoutExtension+".tif")
new FileSaver(impFibre).saveAsTiff(fibreDir.getAbsolutePath()+File.separator+impNameWithoutExtension+".tif")
new FileSaver(impInnerMyelin).saveAsTiff(innerMyelinDir.getAbsolutePath()+File.separator+impNameWithoutExtension+".tif")
new FileSaver(impAxon).saveAsTiff(axonDir.getAbsolutePath()+File.separator+impNameWithoutExtension+".tif")

println "Exported $impNameWithoutExtension masks"
return
