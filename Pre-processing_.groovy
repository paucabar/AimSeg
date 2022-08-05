#@ File(label="Select directory", style="directory") dir

import ij.IJ
import ij.process.ImageConverter
import ij.io.Opener
import ij.ImagePlus
import java.io.File
import groovy.io.FileType

ImagePlus importImage (File inputFile) {
	String imagePath = inputFile.getAbsolutePath()
	def opener = new Opener()
	result = opener.openImage(imagePath)
	return result
}

def preProcessing(ImagePlus imp, float saturatedPixels) {
	int bitDepth = imp.getBitDepth()
	if (bitDepth > 8) {
		def ic = new ImageConverter(imp)
		ic.convertToGray8()
	}
	IJ.run(imp, "Enhance Contrast...", "saturated=$saturatedPixels update")
}

def fileList = []
dir.eachFile(FileType.FILES) {
	fileList << it.name
}

for (i=0; i<fileList.size(); i++) {
	File file = new File (dir, fileList[i])
	ImagePlus imp = importImage(file)
	preProcessing(imp, 0.3)
}
