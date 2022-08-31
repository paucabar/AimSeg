#@ ImagePlus imp

import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import ij.plugin.frame.RoiManager

def ImagePlus impLabel = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
def ImageProcessor ip = impLabel.getProcessor()
def RoiManager rm = RoiManager.getInstance()

rm.getRoisAsArray().eachWithIndex { roi, index ->
	String roiCode = roi.getName().substring(4, 7)
	int roiInt = Integer.parseInt(roiCode);
	ip.setColor(roiInt)
	ip.fill(roi)
}

ip.resetMinAndMax()
IJ.run(impLabel, "glasbey inverted", "")
impLabel.show()