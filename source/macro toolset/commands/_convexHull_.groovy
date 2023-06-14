import ij.IJ
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.process.FloatPolygon
import ij.gui.PolygonRoi

def transferProperties (Roi roi1, Roi roi2) {
    if (roi1==null || roi2==null)
        return
    roi2.setStrokeColor(roi1.getStrokeColor())
    if (roi1.getStroke()!=null)
        roi2.setStroke(roi1.getStroke())
    roi2.setDrawOffset(roi1.getDrawOffset())
    if (roi1.getGroup()!=null)
        roi2.setGroup(roi1.getGroup())
    if (roi1.getStrokeWidth()!=null)
        roi2.setStrokeWidth(roi1.getStrokeWidth())
    if (roi1.getFillColor()!=null)
        roi2.setFillColor(roi1.getFillColor())
}

RoiManager rm = new RoiManager()
rm = rm.getInstance()
int index = rm.getSelectedIndex()
if (index >= 0) {
	Roi roi = rm.getRoi(index)
    rm.select(index)
    FloatPolygon p = roi.getFloatConvexHull()
    Roi roi2 = new PolygonRoi(p, Roi.POLYGON)
    transferProperties(roi, roi2)
    rm.setRoi(roi2, index)
    rm.select(index)
} else {
	IJ.log("No ROI selected")
}