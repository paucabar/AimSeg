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

//create results table
title1 = "ResultsTable";
title2 = "["+title1+"]";
f = title2;
run("Table...", "name="+title2+" width=650 height=500");
print(f, "\\Headings:n\tImage Name\tROI code\tAxon area\tInner Myelin area\tOuter Myelin area");

run("ROI Manager...");
setBatchMode(true);
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
	
	//start analysis
	if (checkIn==true && checkOut==true && checkAxon==true) {
		
		//get image scale
		open(images[i]);
		getVoxelSize(widthGbl, heightGbl, depthGbl, unitGbl);
		
		//inner count masks
		roiManager("Open", dir+File.separator+name+roiInTag);
		run("Select None");
		Stack.getDimensions(widthImg, heightImg, channelsImg, slicesImg, framesImg);
		newImage("InnerMasks", "8-bit black", widthImg, heightImg, slicesImg);
		run("Set Scale...", "distance="+1/widthGbl+" known=1 pixel=1 unit="+unitGbl+" global");
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
				selectImage("check");
				run("Analyze Particles...", "size="+max-0.01+"-"+max+0.01+" show=Masks clear");
				rename("corrected");
				imageCalculator("OR", "InnerMasks","corrected");
				close("corrected");
			} else {
				imageCalculator("OR", "InnerMasks","check");
			}
			close("check");
		}
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		selectImage("InnerMasks");
		run("Invert");
		run("Analyze Particles...", "  show=[Count Masks] add clear");
		roiNumberIn=newArray(roiManager("count"));
		rename("InnerMyelin_CountMasks");
		close("InnerMasks");
		for (j=0; j<roiNumberIn.length; j++) {
			roiManager("select", j);
			roiNumberIn[j]=d2s(j+1, 0);
			while (lengthOf(roiNumberIn[j])<3) {
				roiNumberIn[j]="0"+roiNumberIn[j];
			}
			roiNumberIn[j]="ROI_"+roiNumberIn[j];
			roiManager("rename", roiNumberIn[j]);
		}
		roiManager("Save", dir+name+roiInTag);
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		
		//outer count masks
		roiManager("Open", dir+File.separator+name+roiOutTag);
		newImage("OuterMyelin_CountMasks", "8-bit black", widthImg, heightImg, slicesImg);
		roiNumberOut=newArray(roiManager("count"));
		for (j=0; j<roiNumberOut.length; j++) {
			setForegroundColor(j+1, j+1, j+1);
			roiManager("select", j);
			roiManager("fill");
		}
		selectImage("InnerMyelin_CountMasks");
		roiManager("deselect");
		roiManager("delete");
		run("Select None");

		//axon masks
		roiManager("Open", dir+File.separator+name+roiAxonTag);
		roiManager("Deselect");
		roiManager("Combine");
		run("Create Mask");
		rename("AxonMasks");
		roiManager("deselect");
		roiManager("delete");
		run("Select None");

		//clear
		nIma=nImages;
		for (j=1; j<=nIma; j++) {
			selectImage(j);
			run("Select None");
		}

		//results arrays
		areaAxon=newArray(roiNumberIn.length);
		areaIn=newArray(roiNumberIn.length);
		areaOut=newArray(roiNumberIn.length);
		xIn=newArray(roiNumberIn.length);
		yIn=newArray(roiNumberIn.length);

		//measure inner masks
		run("Set Measurements...", "area feret's redirect=None decimal=2");
		roiManager("Open", dir+File.separator+name+roiInTag);
		for (j=0; j<roiNumberIn.length; j++) {
			roiManager("select", j);
			roiManager("measure");
			areaIn[j]=getResult("Area", 0);
			xIn[j]=getResult("FeretX", 0);
			yIn[j]=getResult("FeretY", 0);
			run("Clear Results");
		}
		roiManager("deselect");
		roiManager("delete");
		run("Select None");

		//measure outer masks
		for (j=0; j<roiNumberIn.length; j++) {
			selectImage("OuterMyelin_CountMasks");
			run("Select None");
			makePoint(xIn[j], yIn[j], "small yellow hybrid");
			run("Set Measurements...", "mean redirect=None decimal=2");
			run("Clear Results");
			run("Measure");
			pixelValue=getResult("Mean", 0);
			run("Clear Results");
			if (pixelValue>0) {
				run("Wand Tool...", "tolerance=0 mode=Legacy");
				selectImage("OuterMyelin_CountMasks");
				doWand(xIn[j], yIn[j]);
				run("Create Mask");
				rename("outline_mask");
				run("Set Measurements...", " area redirect=None decimal=2");
				run("Analyze Particles...", "display add");	
				areaOut[j]=getResult("Area", 0);
				run("Clear Results");
				close("outline_mask");
				roiCount=roiManager("count");
				roiManager("select", roiCount-1);
				roiManager("rename", roiNumberIn[j]);
			} else {
				areaAxon[j]=NaN;
				areaIn[j]=NaN;
				areaOut[j]=NaN;
			}
		}
		roiManager("Save", dir+name+roiOutTag);
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		close("OuterMyelin_CountMasks");

		//measure axoplasm
		for (j=0; j<roiNumberIn.length; j++) {
			axonString=d2s(areaAxon[j], 0);
			if (axonString!="NaN") {
				print(roiNumberIn[j]);
				run("Clear Results");
				selectImage("InnerMyelin_CountMasks");
				setThreshold(j+1, j+1);
				run("Create Mask");
				rename("inner-"+j);
				imageCalculator("AND create", "inner-"+j,"AxonMasks");
				rename("axoplasm-"+j);
				run("Set Measurements...", "area redirect=None decimal=2");
				run("Analyze Particles...", "show=Nothing display");
				particles=nResults;
				if (particles>1) {
					options=newArray(particles);
					for (k=0; k<particles; k++) {
						options[k] = getResult("Area", k);
					}
					Array.getStatistics(options, min, max, mean, stdDev);
					selectImage("axoplasm-"+j);
					run("Clear Results");
					run("Analyze Particles...", "size="+max-0.01+"-"+max+0.01+" show=Masks add");
					rename("corrected");
					roiCount=roiManager("count");
					roiManager("select", roiCount-1);
					roiManager("rename", roiNumberIn[j]);
					areaAxon[j]=max;
					close("corrected");
				} else if (particles == 0) {
					selectImage("inner-"+j);
					run("Analyze Particles...", "show=Nothing add");
					roiCount=roiManager("count");
					roiManager("select", roiCount-1);
					roiManager("rename", roiNumberIn[j]);
					areaAxon[j]=areaIn[j];
				} else {
					selectImage("axoplasm-"+j);
					run("Analyze Particles...", "show=Nothing add");
					roiCount=roiManager("count");
					roiManager("select", roiCount-1);
					roiManager("rename", roiNumberIn[j]);
					areaAxon[j]=getResult("Area", 0);
				}
			close("inner-"+j);
			close("axoplasm-"+j);
			}
		}
		roiManager("Save", dir+name+roiAxonTag);
		roiManager("deselect");
		roiManager("delete");
		run("Select None");
		close("InnerMyelin_CountMasks");
		close("AxonMasks");
		close(images[i]);
		run("Clear Results");

		//print results
		for (j=0; j<roiNumberIn.length; j++) {
			print(f, n+1 + "\t" + name + "\t" + roiNumberIn[j] + "\t" + areaAxon[j] + "\t" + areaIn[j] + "\t" + areaOut[j]);
			n++;
		}
	}
}
setBatchMode(false);

//save results table
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