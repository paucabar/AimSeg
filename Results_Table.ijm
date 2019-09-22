print("\\Clear");
if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
run("Close All");
if (isOpen("ResultsTable")) {
	selectWindow("ResultsTable");
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
print(f, "\\Headings:n\tImage Name\tROI code\tAxon area\tInner Myelin area\tOuter Myelin area");

run("ROI Manager...");
//setBatchMode(true);
n=0;
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
		open(images[i]);
		
		//inner count masks
		roiManager("Open", dir+File.separator+name+roiInTag);
		run("Select None");
		Stack.getDimensions(width, height, channels, slices, frames);
		roiNumberIn=newArray(roiManager("count"));
		for (j=0; j<roiNumberIn.length; j++) {
			roiManager("select", j);
			run("Create Mask");
			rename("check");
			run("Set Measurements...", "area redirect=None decimal=2");
			run("Analyze Particles...", "  show=Nothing display clear");
			particles=nResults;
			if (particles>1) {
				options=newArray(particles);
				for (k=0; k<particles; k++) {
					options[k] = getResult("Area", k);
				}
				Array.getStatistics(options, min, max, mean, stdDev);
				run("Analyze Particles...", "size="+max+"-"+max+" show=Masks");
				rename("corrected");
				roiManager("select", j);
				roiManager("delete");
				run("Create Selection");
				roiManager("add");
				close("corrected");
			}
			close("check");
		}
		roiManager("Deselect");
		roiManager("Combine");
		run("Create Mask");
		rename("InnerMasks");
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		run("Analyze Particles...", "  show=[Count Masks] add");
		rename("InnerMyelin_CountMasks");
		close("InnerMasks");
		for (j=0; j<roiNumberIn.length; j++) {
			roiManager("select", j);
			roiNumberIn[j]=d2s(j+1, 0);
			while (lengthOf(roiNumberIn[j])<3) {
				roiNumberIn[j]="0"+roiNumberIn[j];
			}
			roiManager("rename", "ROI_"+roiNumberIn[j]);
		}
		roiManager("Save", dir+name+roiInTag);
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		
		//outer count masks
		roiManager("Open", dir+File.separator+name+roiOutTag);
		newImage("OuterMyelin_CountMasks", "8-bit black", width, height, slices);
		roiNumberOut=newArray(roiManager("count"));
		for (j=0; j<roiNumberOut.length; j++) {
			roiManager("select", j);
			roiNumberOut[j]=d2s(j+1, 0);
			while (lengthOf(roiNumberOut[j])<3) {
				roiNumberOut[j]="0"+roiNumberOut[j];
			}
			roiManager("rename", "ROI_"+roiNumberOut[j]);
			setForegroundColor(j+1, j+1, j+1);
			roiManager("fill");
		}
		roiManager("Save", dir+name+roiOutTag);
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		//axon masks
		roiManager("Open", dir+File.separator+name+roiAxonTag);
		roiManager("Deselect");
		roiManager("Combine");
		run("Create Mask");
		rename("AxonMasks");
		selectWindow("ROI Manager");
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		for (j=0; j<roiNumberIn.length; j++) {
			run("Set Measurements...", "area redirect="+images[i]+" decimal=2");
			selectImage("InnerMyelin_CountMasks");
			run("Select None");
			run("Duplicate...", "title=ROI_IN"+roiNumberIn[j]);
			setThreshold(j+1, j+1);
			run("Convert to Mask");
			run("Analyze Particles...", "display clear");
			results=nResults;
			if (results==1) {
				areaIn=getResult("Area", 0);
			} else {
				innerResults=newArray(results);
				for (k=0; k<results; k++) {
					innerResults[k]=getResult("Area", k);
				}
				Array.getStatistics(innerResults, minArea, maxArea, meanArea, stdDevArea);
				areaIn=maxArea;
			}
			run("Set Measurements...", "feret's redirect=None decimal=2");
			run("Analyze Particles...", "display clear");
			xIn=getResult("FeretX", 0);
			yIn=getResult("FeretY", 0);
			selectImage("OuterMyelin_CountMasks");
			run("Select None");
			makePoint(xIn, yIn, "small yellow hybrid");
			run("Set Measurements...", "mean redirect=None decimal=2");
			run("Clear Results");
			run("Measure");
			pixelValue=getResult("Mean", 0);
			if (pixelValue>0) {
				run("Wand Tool...", "tolerance=0 mode=Legacy");
				selectImage("OuterMyelin_CountMasks");
				doWand(xIn, yIn);
				run("Create Mask");
				rename("Myelin_outline_mask");
				run("Set Measurements...", "size=10-Infinity area redirect="+images[i]+" decimal=2");
				run("Analyze Particles...", "display clear");		
				areaOut=getResult("Area", 0);
				imageCalculator("AND create", "ROI_IN"+roiNumberIn[j],"AxonMasks");
				rename("Axon");
				run("Set Measurements...", "area redirect="+images[i]+" decimal=2");
				run("Analyze Particles...", "display clear");
				results=nResults;
				close("Axon");
				close("Myelin_outline_mask");
				if (results==1) {
					areaAxon=getResult("Area", 0);
				} else if (results==0) {
					areaAxon=areaIn;
				} else {
					axonResults=newArray(results);
					for (k=0; k<results; k++) {
						axonResults[k]=getResult("Area", k);
					}
					Array.getStatistics(axonResults, minArea, maxArea, meanArea, stdDevArea);
					areaAxon=maxArea;
				}
				print(f, n+1 + "\t" + name + "\t" + roiNumberIn[j] + "\t"+areaAxon+"\t" + areaIn + "\t" + areaOut);
				n++;
			} else {
				areaOut=NaN;
				areaIn=NaN;
				areaAxon=NaN;
				print(f, n+1 + "\t" + name + "\t" + roiNumberIn[j] + "\t" + areaAxon + "\t" + areaIn + "\t" + areaOut);
				n++;
			}
			close("ROI_IN"+roiNumberIn[j]);
			if (j+1==roiNumberIn.length) {
				close(images[i]);
				close("InnerMyelin_CountMasks");
				close("OuterMyelin_CountMasks");
				close("AxonMasks");
				selectWindow("Results");
				run("Close");
			}
		}
	} else {
		print (images[i], "skipped: some ROI sets are missing");
	}
}
setBatchMode(false);
getDateAndTime(year, month, dayOfWeek, dayOfMonth, hour, minute, second, msec);
dayOfMonth=d2s(dayOfMonth, 0);
while (lengthOf(dayOfMonth) < 2) {
	dayOfMonth="0"+dayOfMonth;
}
month+=1;
month=d2s(month, 0);
while (lengthOf(month) < 2) {
	month="0"+month;
}
hour=d2s(hour, 0);
while (lengthOf(hour) < 2) {
	hour="0"+hour;
}
minute=d2s(minute, 0);
while (lengthOf(minute) < 2) {
	minute="0"+minute;
}
selectWindow(title1);
saveAs("Text", dir+File.separator+title1+dayOfMonth+month+year+"_"+hour+minute+".csv");