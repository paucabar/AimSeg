#@ ImagePlus imp

import ij.plugin.frame.RoiManager
import ij.gui.Roi
import java.awt.Color

RoiManager rm = new RoiManager()
rm = rm.getInstance()
rm.getRoisAsArray().eachWithIndex { roi, index ->
	Color fillColor = roi.getFillColor()
	if (fillColor == null) {
		Color strokeColor = roi.getStrokeColor()
		if (strokeColor == null) strokeColor = roi.getColor() // default color
		Color cTransparent = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 75)
		roi.setFillColor(cTransparent)
		rm.setRoi(roi, index)
	} else {
		roi.setFillColor(null)
		rm.setRoi(roi, index)
	}
}
return