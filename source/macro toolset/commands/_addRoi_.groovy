#@ ImagePlus imp

import ij.IJ
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import java.awt.Color

Roi roi = imp.getRoi()
if (roi != null) {
	roi.setGroup(2)
	roi.setLineWidth(1)
	RoiManager rm = new RoiManager()
	rm = rm.getInstance()
	Color fillColor = rm.getRoi(0).getFillColor()
	if (fillColor != null) {
		Color strokeColor = roi.getStrokeColor()
		if (strokeColor == null) strokeColor = roi.getColor() // default color
		Color cTransparent = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), fillColor.getAlpha())
		roi.setFillColor(cTransparent)
	}
	rm.addRoi(roi)
} else {
	IJ.log("No ROI selected")
}