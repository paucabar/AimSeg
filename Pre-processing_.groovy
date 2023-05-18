#@ File(label="Select directory", style="directory") dir
#@ float(label="Downsample", value=2.0, persist=true) downsample
#@ boolean(label="Normalize", value=false, persist=true) normalize

import ij.IJ
import ij.process.ImageConverter
import ij.io.Opener
import ij.ImagePlus
import ij.process.ImageProcessor
import java.io.File
import groovy.io.FileType

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
	ImageProcessor ip = imp.getProcessor().resize(Math.ceil(imp.getWidth()/downsample).intValue(), Math.ceil(imp.getHeight()/downsample).intValue(), false)
	ImagePlus result = new ImagePlus(imp.getTitle(), ip)
	return result
}

def preProcessing(ImagePlus imp, float saturatedPixels, float downsample) {
	if (normalize) {
		IJ.run(imp, "Enhance Contrast...", "saturated=$saturatedPixels update")
	}
	if (downsample != 1.0) {
		imp = resizeImage(imp, downsample)
	}
	int bitDepth = imp.getBitDepth()
	if (bitDepth > 8) {
		def ic = new ImageConverter(imp)
		//ic.setDoScaling(true)
		ic.convertToGray8()
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
	// Shouldn't happen if we only have images - but check anyway
	if (imp == null)
		continue
	imp = preProcessing(imp, 0.1, downsample)
	String title = imp.getShortTitle()
	println "${->title} processed"
	path = new File(outDir, "${->title}.tif").getAbsolutePath()
	ij.IJ.save(imp, path)
}