#@ String (label="Binary Operation", choices={"Erode", "Dilate"}) operation

import ij.IJ
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.plugin.RoiEnlarger

def run(double pixels) {
	RoiManager rm = new RoiManager()
	rm = rm.getInstance()
	int index = rm.getSelectedIndex()
	if (index >= 0) {
		Roi roi = rm.getRoi(index)
	    RoiEnlarger enlarger = new RoiEnlarger()
	    resized = enlarger.enlarge(roi, pixels)
	    rm.setRoi(resized, index)
	    rm.select(index)
	} else {
		IJ.log("No ROI selected")
	}
}

if (operation == "Erode") {
	run(-1)
} else {
	run(1)
}
