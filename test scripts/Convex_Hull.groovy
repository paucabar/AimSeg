#@ ImagePlus imp

import ij.IJ
import ij.ImagePlus
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.process.FloatPolygon
import ij.gui.PolygonRoi

private static void transferProperties(Roi roi1, Roi roi2) {
	if (roi1==null || roi2==null)
		return;
	roi2.setStrokeColor(roi1.getStrokeColor());
	if (roi1.getStroke()!=null)
		roi2.setStroke(roi1.getStroke());
	roi2.setDrawOffset(roi1.getDrawOffset());
	if (roi1.getGroup()!=null)
		roi2.setGroup(roi1.getGroup());
}

private static void convexHull(ImagePlus imp, RoiManager rm) {
	int roiCount = rm.getCount()
	println "$roiCount selected ROIs"
	for (i in 0..roiCount-1) {
		rm.select(0)
		Roi roi = imp.getRoi()
		FloatPolygon p = roi.getFloatConvexHull();
			if (p!=null) {
				Roi roi2 = new PolygonRoi(p, Roi.POLYGON);
				transferProperties(roi, roi2);
				rm.addRoi(roi2)
				rm.select(0)
				rm.runCommand(imp,"Delete")
			}
	}
}

rm = RoiManager.getInstance()
convexHull(imp, rm)