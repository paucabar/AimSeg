function roundMultiple(x, multiple) {
    return round(x/multiple)*multiple;
}

function updateBrushSize(value, multiple) {
	brushWidth = call("ij.gui.Toolbar.getBrushSize");
	brushWidth = parseInt(brushWidth);
	call("ij.gui.Toolbar.setBrushSize", roundMultiple(brushWidth + value, multiple));
}

macro "Select Brush Tool [1]" {
	setTool("brush");
}

macro "Select Freehand Tool [2]" {
	setTool("freehand");
}

macro "Select Wand Tool [3]" {
	setTool("wand");
}

macro "Select None [n0]" {
	run("Select None");
}

macro "Toggle ShowAllROIs [r]" {
	run(" toggleShowAllROIs ", "")
}

macro "Toggle ShowRoiManager [n]" {
	run(" toggleShowRoiManager ", "")
}

macro "Toggle FillRois [f]" {
	run(" toggleFillRois ", "")
}

macro "Toggle Roi Group [g]" {
	run(" toggleRoiGroup ", "")
}

macro "Add Roi [a]" {
	run(" addRoi ", "")
	run("Select None");
}

macro "Add Roi 2 [t]" {
	run(" addRoi ", "")
	run("Select None");
}

macro "Erode Roi[e]" {
	run(" roiEnlarger ", "operation=Erode")
}

macro "Dilate Roi[d]" {
	run(" roiEnlarger ", "operation=Dilate")
}

macro "Increase Brush Size [n8]" {
	updateBrushSize(3, 3);
}

macro "Decrease Brush Size [n2]" {
	updateBrushSize(-3, 3);
}

macro "Delete ROI [D]" {
	if(roiManager("index") != -1){
		roiManager("Delete");
		run("Select None");
	}
}

macro "Update Roi [u]" {
	roiManager("Update");
}

macro "Split Shape Roi [s]" {
	run(" splitShapeRoi ", "")
}

macro "Merge Rois [m]" {
	run(" mergeRois ", "")
}

macro "Convex Hull [x]" {
	run(" convexHull ", "")
}
