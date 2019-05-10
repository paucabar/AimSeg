if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
run("Close All");

setOption("ExpandableArrays", true);
objectTag="_Object Predictions.tiff";
probabilitiesTag="_Probabilities.h5";
roiInTag="_RoiSet_IN.zip";
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

for (i=0; i<count; i++) {
	open(images[i]);
	name=substring(images[i], 0, lastIndexOf(images[i], "."));
	roiManager("Open", dir+File.separator+name+roiInTag);
	Stack.getDimensions(width, height, channels, slices, frames);
	newImage("CountMasks", "8-bit black", width, height, slices);
	roiNumber=roiManager("count");
	for (i=0; i<roiNumber; i++) {
		roiManager("select", i);
		roiCode=d2s(i, 0);
		while (lengthOf(roiCode)<3) {
			roiCode="0"+roiCode;
		}
		roiManager("rename", "ROI_"+roiCode);
		setForegroundColor(i, i, i);
		roiManager("fill");
	}
	exit()
}