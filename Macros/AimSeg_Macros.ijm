// macro to switch the color of the selected ROI
macro "Toggle ROI group [q]" {
	index=roiManager("index");
	if(index >= 0){
		group=Roi.getGroup;
		if(group!=2){
			Roi.setGroup(2);
		} else {
			Roi.setGroup(1);
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