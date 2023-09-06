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
		if (strokeColor == null) strokeColor = roi.getColor() // get default color if none is assigned
		Color cTransparent = new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 75)
		roi.setFillColor(cTransparent)
		rm.setRoi(roi, index)
	} else {
		fillColor = roi.getFillColor()
		Color edgeColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 254)
		roi.setFillColor(null)
		roi.setStrokeColor(edgeColor)
		rm.setRoi(roi, index)
	}
}
return