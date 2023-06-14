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
int[] indexes = rm.getSelectedIndexes()
if (indexes.size() > 1) {
	
	Roi[] rois = rm.getSelectedRoisAsArray()
	
	
	// merge 
	ShapeRoi s1 = new ShapeRoi(rois[0])
	for(i in (1..(rois.size())-1)) {
		ShapeRoi s2 = new ShapeRoi(rois[i])
		s1 = s1.or(s2)
	}
	transferProperties(rois[0], s1)
	rm.addRoi(s1)
	rm.setSelectedIndexes(indexes)
	rm.runCommand("Delete") // delete selected rois
} else {
	IJ.log("Requires at least 2 selected Rois")
}
return