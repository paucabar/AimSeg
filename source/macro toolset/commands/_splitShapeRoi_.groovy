import ij.IJ
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.gui.ShapeRoi

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
	if (roi instanceof ShapeRoi) {
		Roi[] rois = ((ShapeRoi)roi).getRois()
		if (rois.size() > 1) {
			rm.select(index)
			rm.runCommand("Delete") // delate shape roi
			
			// get merge
			ShapeRoi s1 = new ShapeRoi(roi)
			for(i in (1..(rois.size())-1)) {
				ShapeRoi s2 = new ShapeRoi(rois[i])
				s1 = s1.or(s2)
			}
			
			// for each roi in merge rois get s1 intersection
			Roi[] m_rois = ((ShapeRoi)s1).getRois()
			m_rois.each {
				ShapeRoi s_it = new ShapeRoi(it)
				s_it.and(roi)
				transferProperties(roi, s_it)
				rm.addRoi(s_it)
			}
		} else {
			IJ.log("Not a composite Roi")
		}
	} else {
		IJ.log("Not a composite Roi")
	}
} else {
	IJ.log("No ROI selected")
}
return