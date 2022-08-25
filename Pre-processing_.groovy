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

// create file list
def fileList = []
dir.eachFile(FileType.FILES) {
	fileList << it.name
}

// create output folder
File outDir = new File(dir.getParentFile(), dir.getName()+'_pre-processed');
if (outDir.getParentFile() != null) {
  outDir.mkdirs();
}
outDir.createNewFile();

// import, process and save
for (i=0; i<fileList.size(); i++) {
	File file = new File (dir, fileList[i])
	ImagePlus imp = importImage(file)
	preProcessing(imp, 0.3)
	String title = imp.getShortTitle()
	File path = new File(outDir, "labels_${->title}.tif").getAbsolutePath()
	ij.IJ.save(imp, path)
}
