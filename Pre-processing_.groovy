#@ ImagePlus imp

import ij.IJ
import ij.process.ImageConverter
import ij.ImagePlus

def preProcessing(ImagePlus imp, float saturatedPixels) {
	int bitDepth = imp.getBitDepth()
	if (bitDepth > 8) {
		def ic = new ImageConverter(imp)
		ic.convertToGray8()
	}
	IJ.run(imp, "Enhance Contrast...", "saturated=$saturatedPixels update")
}

preProcessing(imp, 0.3)