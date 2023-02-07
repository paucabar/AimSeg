#@ File(label="Select directory", style="directory") dir

import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import java.io.File
import groovy.io.FileType
import ij.gui.Roi

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

ImagePlus[] cropImage (ImagePlus imp) {
	int[] dims = imp.getDimensions() // width, height, nChannels, nSlices, nFrames
	Roi roi1 = new Roi(0, 0, dims[0]/2, dims[1]/2) //top-left tile
	Roi roi2 = new Roi(dims[0]/2 + 1, 0, dims[0], dims[1]/2) //top-right tile
	Roi roi3 = new Roi(0, dims[1] / 2 + 1, dims[0] / 2, dims[1]/2) //bottom-left tile
	Roi roi4 = new Roi(dims[0]/2 + 1, dims[1] / 2 + 1, dims[0] / 2, dims[1] / 2) //bottom-right tile
	ImagePlus[] crops = imp.crop((Roi[])[roi1, roi2, roi3, roi4])
	return crops
}


// create file list
def fileList = []
dir.eachFile(FileType.FILES) {
	fileList << it.name
}

// create output folder
File outDir = new File(dir.getParentFile(), dir.getName()+'_crops')
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
	ImagePlus [] crop = cropImage(imp)
	String title = imp.getShortTitle()
	crop.eachWithIndex {it,index->
		path = new File(outDir, "${->title}-"+index+".tif").getAbsolutePath()
		ij.IJ.save(it, path)
	}
	println "${->title} processed"
}

