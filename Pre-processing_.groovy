#@ImagePlus imp

import ij.IJ
import ij.process.ImageConverter
import ij.ImagePlus

int bitDepth = imp.getBitDepth()
if (bitDepth > 8) {
	def ic = new ImageConverter(imp)
	ic.convertToGray8()
}
float saturatedPixels = 0.3
IJ.run(imp, "Enhance Contrast...", "saturated=$saturatedPixels update")