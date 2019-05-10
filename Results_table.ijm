if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
run("Close All");

setOption("ExpandableArrays", true);
objectTag="_Object Predictions.tiff";
probabilitiesTag="_Probabilities.h5";
roiInTag="_RoiSet_IN.zip";
roiOutTag="_RoiSet_OUT.zip";
roiAxonTag="_RoiSet_AXON.zip";
dir=getDirectory("Choose a Directory");
list=getFileList(dir);
images=newArray;
count=0;
for (i=0; i<list.length; i++) {
	if (indexOf(list[i], objectTag) == -1 && indexOf(list[i], probabilitiesTag) == -1 && endsWith(list[i], ".tif") == 1) {
		images[count]=list[i];
		count++;
	}
}
if (count==0) {
	exit("No TIF files are found")
}

//create results table
title1 = "ResultsTable";
title2 = "["+title1+"]";
f = title2;
run("Table...", "name="+title2+" width=650 height=500");
print(f, "\\Headings:n\tImage Name\tROI code (inner myelin)\tAxon area\tInner Myelin area\tOuter Myelin area");

//open images
for (i=0; i<count; i++) {
	name=substring(images[i], 0, lastIndexOf(images[i], "."));
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
			open(images[i]);
			//inner count masks
			roiManager("Open", dir+File.separator+name+roiInTag);
			Stack.getDimensions(width, height, channels, slices, frames);
			newImage("InnerMyelin_CountMasks", "8-bit black", width, height, slices);
			roiNumberIn=newArray(roiManager("count"));
			for (j=0; j<roiNumberIn.length; j++) {
				roiManager("select", j);
				roiNumberIn[j]=d2s(j, 0);
				while (lengthOf(roiNumberIn[j])<3) {
					roiNumberIn[j]="0"+roiNumberIn[j];
				}
				roiManager("rename", "ROI_"+roiNumberIn[j]);
				setForegroundColor(j, j, j);
				roiManager("fill");
			}
			roiManager("Save", dir+name+roiInTag);
			roiManager("deselect");
			roiManager("delete");
			//outer count masks
			roiManager("Open", dir+File.separator+name+roiOutTag);
			newImage("OuterMyelin_CountMasks", "8-bit black", width, height, slices);
			roiNumberOut=newArray(roiManager("count"));
			for (j=0; j<roiNumberOut.length; j++) {
				roiManager("select", j);
				roiNumberOut[j]=d2s(j, 0);
				while (lengthOf(roiNumberOut[j])<3) {
					roiNumberOut[j]="0"+roiNumberOut[j];
				}
				roiManager("rename", "ROI_"+roiNumberOut[j]);
				setForegroundColor(j, j, j);
				roiManager("fill");
			}
			roiManager("Save", dir+name+roiOutTag);
			roiManager("deselect");
			roiManager("delete");
			run("Close All");
		} else {
			print (images[i], "skipped: some ROI sets are missing");
		}
}