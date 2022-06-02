// macro to switch the color of the selected ROI
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
