#@ ImagePlus imp

import ij.ImagePlus
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.process.ImageProcessor
import ij.plugin.filter.ThresholdToSelection
import ij.gui.ShapeRoi
import ij.gui.PolygonRoi

/**
 * Gets the intersection of each Roi and the corresponding label at imp
 */
def roiAndLabel(ImagePlus imp, RoiManager rm) {
	rm.getRoisAsArray().eachWithIndex { roi, index ->
		def codeInt = roi.getName() as int
		ip = imp.getProcessor()
		ip.setThreshold (codeInt, codeInt, ImageProcessor.NO_LUT_UPDATE)		
		def tts = new ThresholdToSelection()
		roiLabel = tts.convert(ip)
		if(roiLabel != null) {
			String name = roi.getName()
			s1 = new ShapeRoi(roi)
			s2 = new ShapeRoi(roiLabel)
			s3 = s1.and(s2)
			s3.setName(name)
			transferProperties(roi, s3)
			rm.setRoi(s3, index)
		}
	}
}

def transferProperties (Roi roi1, Roi roi2) {
	if (roi1==null || roi2==null)
		return
		roi2.setStrokeColor(roi1.getStrokeColor())
	if (roi1.getStroke()!=null)
		roi2.setStroke(roi1.getStroke())
		roi2.setDrawOffset(roi1.getDrawOffset())
	if (roi1.getGroup()!=null)
		roi2.setGroup(roi1.getGroup())
}

roiAndLabel(imp, RoiManager.getInstance())