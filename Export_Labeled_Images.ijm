//clean up
print("\\Clear");
if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
if (isOpen("ResultsTable")) {
	selectWindow("ResultsTable");
	run("Close");
}
run("Close All");


//setOptions
setOption("BlackBackground", false); //disables the Process>Binary>Options "Black background" option
setOption("ExpandableArrays", true);
run("ROI Manager...");
//setBatchMode(true);

//some variables
objectTag="_Object Predictions.tiff";
probabilitiesTag="_Probabilities.h5";
roiInTag="_RoiSet_IN.zip";
roiOutTag="_RoiSet_OUT.zip";
roiAxonTag="_RoiSet_AXON.zip";

//get directory and file list
dir=getDirectory("Choose a Directory");
list=getFileList(dir);

//count tifs
images=newArray;
count=0;
for (i=0; i<list.length; i++) {
	if (indexOf(list[i], objectTag) == -1 && indexOf(list[i], probabilitiesTag) == -1 && endsWith(list[i], ".tif") == 1) {
		images[count]=list[i];
		count++;
	}
}

//check that the folder contains tif files
if (count==0) {
	exit("No tif files are found")
}

//start processing
for (i=0; i<count; i++) {
	name=substring(images[i], 0, lastIndexOf(images[i], "."));	
	//skip images missing some ROI set
	checkIn=false;
	checkOut=false;
	checkAxon=false;
	for (j=0; j<list.length; j++) {
		if (list[j] == name+roiInTag) {
			checkIn=true;
		} else if (list[j] == name+roiOutTag) {
			checkOut=true;
		} else if (list[j] == name+roiAxonTag) {
			checkAxon=true;
		}
	}
	if (checkIn==true && checkOut==true && checkAxon==true) {		
		//(ROIs to binary masks
		open(images[i]);
		binaryMasks(roiInTag, "inner");
		binaryMasks(roiOutTag, "outer");
		binaryMasks(roiAxonTag, "axon");
	
		//myelin and inner tongue masks
		imageCalculator("XOR create", "Mask_outer", "Mask_inner");
		rename("Mask_myelin");
		imageCalculator("XOR create", "Mask_axon", "Mask_inner");
		rename("Mask_innertongue");

		//create black image
		selectImage(images[i]);
		Stack.getDimensions(widthImg, heightImg, channelsImg, slicesImg, framesImg);
		newImage("Labeled Image", "8-bit black", widthImg, heightImg, slicesImg);
		labeledImage("Mask_axon", 1);
		labeledImage("Mask_innertongue", 2);
		labeledImage("Mask_myelin", 3);
		run("glasbey on dark ");
		//setBatchMode(false);
		exit;
	}
}

function binaryMasks(roi_tag, name_tag) {
	selectImage(images[i]);
	roiManager("Open", dir+File.separator+name+roi_tag);
	roiManager("Deselect");
	roiManager("Combine");
	run("Create Mask");
	rename("Mask_"+name_tag);
	roiManager("deselect");
	roiManager("delete");
	run("Select None");
}

function labeledImage(image, value){
	selectImage(image);
	run("Create Selection");
	roiManager("Add");
	setForegroundColor(value, value, value);
	selectImage("Labeled Image");
	roiManager("Fill");
	roiManager("deselect");
	roiManager("delete");
	run("Select None");
}
