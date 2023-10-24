#@ File(label="Select directory", style="directory") dir
#@ Float(label="Downsample", value=1, min=0.05, max=10, stepSize=0.05, style="format:#.##", persist=true) downsampleFloat
#@ boolean(label="Normalize", value=false, persist=true) normalize
#@ Float(label="% Sat Pixels", value=1, min=0.01, max=10, stepSize=0.25, style="format:#.##", persist=true) saturatedFloat

import ij.IJ
import ij.process.ImageConverter
import ij.io.Opener
import ij.ImagePlus
import ij.plugin.ContrastEnhancer
import ij.process.ImageProcessor
import java.io.File
import groovy.io.FileType

// Float conversion
float downsample = downsampleFloat
float saturated = saturatedFloat

ImagePlus importImage (File inputFile) {
	String imagePath = inputFile.getAbsolutePath()
	def opener = new Opener()
	// Use DM3_Reader.java is available (and required)
	opener.useHandleExtraFileTypes = true
	String extension = imagePath[imagePath.lastIndexOf('.')+1..-1]
	println "Importing $extension file"
	result = opener.openUsingBioFormats(imagePath)
	return result
}

ImagePlus resizeImage(ImagePlus imp, float downsample) {
	ImagePlus result = imp.resize(Math.round(imp.getWidth()/downsample).intValue(), Math.round(imp.getHeight()/downsample).intValue(), "none")
	return result
}

def preProcessing(ImagePlus imp, float saturated, float downsample) {
	if (normalize) {
		int bitDepth = imp.getBitDepth()
		if (bitDepth != 32) {
			ImageConverter ic = new ImageConverter(imp)
			//ic.setDoScaling(true)
			ic.convertToGray32()
		}
		ContrastEnhancer ce = new ContrastEnhancer()
		ce.setNormalize(true)
		ce.stretchHistogram(imp, saturated)
		imp.getProcessor().resetMinAndMax()
	}
	if (downsample != 1.0) {
		imp = resizeImage(imp, downsample)
	}
	//imp.show()
	return imp
}

// create file list
def fileList = []
dir.eachFile(FileType.FILES) {
	fileList << it.name
}

// create output folder
File outDir = new File(dir.getParentFile(), dir.getName()+'_pre-processed')
if (outDir.getParentFile() != null) {
  outDir.mkdirs()
}
outDir.createNewFile()

// import, process and save
for (i=0; i<fileList.size(); i++) {
	File file = new File (dir, fileList[i])
	// Skip hidden files
	if (file.isHidden())
		continue
	ImagePlus imp = importImage(file)
	String title = imp.getShortTitle()
	println "Processing ${->title}"
	// Shouldn't happen if we only have images - but check anyway
	if (imp == null)
		continue
	imp = preProcessing(imp, saturated, downsample)
	path = new File(outDir, "${->title}.tif").getAbsolutePath()
	ij.IJ.save(imp, path)
}