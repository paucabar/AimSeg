#@ File(label="Select directory", style="directory") dir
#@ boolean(label="Normalize", value=false, persist=true) normalize

import ij.IJ
import ij.process.ImageConverter
import ij.io.Opener
import ij.ImagePlus
import java.io.File
import groovy.io.FileType

ImagePlus importImage (File inputFile) {
	String imagePath = inputFile.getAbsolutePath()
	def opener = new Opener()
	// Use DM3_Reader.java is available (and required)
	opener.useHandleExtraFileTypes = true
	result = opener.openImage(imagePath)
//	opener.open(imagePath)
	return result
}

def preProcessing(ImagePlus imp, float saturatedPixels) {
	if (normalize) {
		IJ.run(imp, "Enhance Contrast...", "saturated=$saturatedPixels update")
	}
	int bitDepth = imp.getBitDepth()
	if (bitDepth > 8) {
		def ic = new ImageConverter(imp)
		//ic.setDoScaling(true)
		ic.convertToGray8()
	}
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
	preProcessing(imp, 0.3)
	String title = imp.getShortTitle()
	println "${->title} processed"
	path = new File(outDir, "${->title}.tif").getAbsolutePath()
	ij.IJ.save(imp, path)
}