macro "Select Brush Tool [1]" {
	setTool("brush");
}

macro "Select Freehand Tool [2]" {
	setTool("freehand");
}

macro "Select Wand Tool [3]" {
	setTool("wand");
}

macro "Select None [0]" {
	run("Select None");
}

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
	count=roiManager("count");
	roiManager("select", count-1);
	RoiManager.setGroup(2);
	roiManager("Set Line Width", 5);
}

macro "Delete ROI [d]" {
	roiManager("Delete");
	run("Select None");
}

macro "Update ROI [u]" {
	roiManager("Update");
}

macro "Split ROI [z]" {
	count1=roiManager("count");
	roiManager("Split");
	roiManager("Delete");
	count2=roiManager("count");
	for (i = count1-1; i < count2; i++) {
		roiManager("select", i);
		RoiManager.setGroup(2);
		roiManager("Set Line Width", 5);
		roiManager("deselect");
	}
}

macro "Convex Hull [c]" {
	index=roiManager("index");
	run("Convex Hull");
	roiManager("add");
	count=roiManager("count");
	roiManager("select", count-1);
	RoiManager.setGroup(2);
	roiManager("Set Line Width", 5);
	roiManager("select", index);
	roiManager("delete");
	run("Select None");
}
