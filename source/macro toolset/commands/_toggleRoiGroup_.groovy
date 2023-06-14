#@ ImagePlus imp

import ij.IJ
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import java.awt.Color

RoiManager rm = new RoiManager()
rm = rm.getInstance()
int index = rm.getSelectedIndex()
if (index >= 0) {
	Roi roi = rm.getRoi(index)
	int group = roi.getGroup()
	group!=2 ? roi.setGroup(2) : roi.setGroup(1)
	if (roi.getFillColor() != null) {
		Color strokeColor = roi.getStrokeColor()
		roi.setFillColor(new Color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), 75))
	}
} else {
	IJ.log("No ROI selected")
}