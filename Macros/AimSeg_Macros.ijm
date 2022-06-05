// macro to switch the color of the selected ROI
macro "Toggle ROI color [q]" {
	index=roiManager("index");
	if(index >= 0){
		color=Roi.getStrokeColor;
		if(color!="red"){
			Roi.setStrokeColor("red");
		} else {
			Roi.setStrokeColor("yellow");
		}
	} else {
		showMessage("No ROI selected");
	}
}

macro "Add ROI [a]" {
	roiManager("Add");
}

macro "Delete ROI [d]" {
	roiManager("Delete");
	run("Select None");
}

macro "Update ROI [u]" {
	roiManager("Update");
}

macro "Split ROI [z]" {
	roiManager("Split");
	roiManager("Delete");
}