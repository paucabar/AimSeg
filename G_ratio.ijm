
//Check if the ilastik plugin is installed
List.setCommands; 
if (List.get("Import HDF5")=="" && List.get("Add Borders")=="") { 
	options=newArray("Yes, update", "No, exit");
	Dialog.create("Update");
	Dialog.addRadioButtonGroup("In order to run this macro you will need to install the \"ilastik\" and \"Morphology\" plugins.\nDo you want to install the \"ilastik\" and \"Morphology\" plugins now?", options, 1, 2, options[0]);
	html = "<html>"
		+"Follow the next steps at the <b>ImageJ updater</b>:<br>"
		+"<H5><i>The updater will be automaically opened once you select \"Yes, update\"</i></H5><br>"
		+"1. Click on \"Manage update sites\"<br>"
		+"2. Check the \"ilastik\" site<br>"
		+"3. Check the \"Morphology\" site<br>"
		+"4. Click on \"Close\" (<b>DO NOT</b> click on \"Add update site\")<br>"
		+"5. Click on \"Apply changes\"<br>"
		+"6. Restart ImageJ";
	Dialog.addHelp(html);
	Dialog.show();
	answer=Dialog.getRadioButton();
	if (answer==options[0]) {
		run("Update...");
		exit()
	} else {
		exit()
	}
} else if (List.get("Import HDF5")=="") {
	options=newArray("Yes, update", "No, exit");
	Dialog.create("Update");
	Dialog.addRadioButtonGroup("In order to run this macro you will need to install the \"ilastik\" plugin.\nDo you want to install the \"ilastik\" plugin now?", options, 1, 2, options[0]);
	html = "<html>"
		+"Follow the next steps at the <b>ImageJ updater</b>:<br>"
		+"<H5><i>The updater will be automaically opened once you select \"Yes, update\"</i></H5><br>"
		+"1. Click on \"Manage update sites\"<br>"
		+"2. Check the \"ilastik\" site<br>"
		+"3. Click on \"Close\" (<b>DO NOT</b> click on \"Add update site\")<br>"
		+"4. Click on \"Apply changes\"<br>"
		+"5. Restart ImageJ";
	Dialog.addHelp(html);
	Dialog.show();
	answer=Dialog.getRadioButton();
	if (answer==options[0]) {
		run("Update...");
		exit()
	} else {
		exit()
	}
} else if (List.get("Add Borders")=="") {
	options=newArray("Yes, update", "No, exit");
	Dialog.create("Update");
	Dialog.addRadioButtonGroup("In order to run this macro you will need to install the \"Morphology\" plugin.\nDo you want to install the \"Morphology\" plugin now?", options, 1, 2, options[0]);
	html = "<html>"
		+"Follow the next steps at the <b>ImageJ updater</b>:<br>"
		+"<H5><i>The updater will be automaically opened once you select \"Yes, update\"</i></H5><br>"
		+"1. Click on \"Manage update sites\"<br>"
		+"2. Check the \"Morphology\" site<br>"
		+"3. Click on \"Close\" (<b>DO NOT</b> click on \"Add update site\")<br>"
		+"4. Click on \"Apply changes\"<br>"
		+"5. Restart ImageJ";
	Dialog.addHelp(html);
	Dialog.show();
	answer=Dialog.getRadioButton();
	if (answer==options[0]) {
		run("Update...");
		exit()
	} else {
		exit()
	}
}

if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
run("Close All");

setOption("ExpandableArrays", true);
objectTag="_Object Predictions.tiff";
probabilitiesTag="_Probabilities.h5";
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

Dialog.create("Field of view");
Dialog.addChoice("Process", images);
Dialog.show();
imageName=Dialog.getChoice();

run("Colors...", "foreground=white background=black selection=yellow");
open(dir+File.separator+imageName);
run("8-bit");
run("Import HDF5", "select="+dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+probabilitiesTag+" datasetname=[/exported_data: (4096, 4096, 3) float32] axisorder=yxc");
rename("3-channels");
run("Duplicate...", "title=Myelin duplicate channels=1");
setThreshold(0.2000, 1000000000000000000000000000000.0000);
setOption("BlackBackground", false);
run("Convert to Mask");
run("Duplicate...", "title=Myelin_inverted");
run("Invert");
run("Analyze Particles...", "  show=Masks exclude");
rename("Myelin_inverted_excludedEdges");
run("Options...", "iterations=2 count=1 do=Close");
run("Fill Holes");
imageCalculator("XOR create", "Myelin_inverted","Myelin_inverted_excludedEdges");
selectWindow("Result of Myelin_inverted");
run("Analyze Particles...", "  circularity=0.30-1.00 show=Masks");
rename("Myelin_inverted_edges");
imageCalculator("OR create", "Myelin_inverted_excludedEdges","Myelin_inverted_edges");
run("Analyze Particles...", "size=10000-Infinity circularity=0.4-1.00 show=Masks add");
name=substring(imageName, 0, indexOf(imageName, "."))+"_Inner_masks";
rename(name);

close("3-channels");
close("Myelin_inverted_excludedEdges");
close("Result of Myelin_inverted");
close("Myelin_inverted_edges");
close("Result of Myelin_inverted_excludedEdges");

imageCalculator("OR create", "Myelin_inverted", name);
rename("All_masks");
imageCalculator("XOR create", "All_masks", name);
rename("Other_masks");
run("Merge Channels...", "c4="+imageName+" c3=Other_masks c1="+name+" create keep");
rename("Composite_inner");
close(name);
close("All_masks");
close("Other_masks");
Stack.setChannel(2);
setTool("wand");
run("Channels Tool...");
roiManager("Show All");
titleWFU="Edit ROIs";
msgWFU="If necessary, use the \"ROI Manager\" to edit\nthe output. Click \"OK\" once you finish";
waitForUser(titleWFU, msgWFU);

roiManager("Save", dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+"_RoiSet_IN.zip");
roiManager("Deselect");
roiManager("Combine");
run("Create Mask");
rename(name);
close("Composite_inner");
options=newArray("Yes, visualize", "No, next step");
answer=options[0];
do {
	Dialog.create("Visualize");
	Dialog.addRadioButtonGroup("Do you want to visualize your edition?", options, 1, 2, options[0]);
	Dialog.show();
	answer=Dialog.getRadioButton();
	if (answer==options[0]) {
		imageCalculator("OR create", "Myelin_inverted", name);
		rename("All_masks");
		imageCalculator("XOR create", "All_masks", name);
		rename("Other_masks");
		run("Merge Channels...", "c4="+imageName+" c3=Other_masks c1="+name+" create keep");
		rename("Composite_inner");
		close(name);
		close("All_masks");
		close("Other_masks");
		Stack.setChannel(2);
		setTool("wand");
		roiManager("Show All");
		waitForUser(titleWFU, msgWFU);
		roiManager("Save", dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+"_RoiSet_IN.zip");
		roiManager("Deselect");
		roiManager("Combine");
		run("Create Mask");
		rename(name);
		close("Composite_inner");
	}
} while (answer==options[0]);



if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
selectImage(name);
run("Duplicate...", "title=Separators");
run("Voronoi");
setThreshold(1, 255);
setOption("BlackBackground", false);
run("Convert to Mask");
run("Options...", "iterations=2 count=1 do=Dilate");
run("Invert");
imageCalculator("AND create", "Myelin",name);
selectWindow("Result of Myelin");
imageCalculator("XOR create", "Myelin","Result of Myelin");
rename("Myelin_clean");
close("Result of Myelin");
imageCalculator("AND create", "Myelin_clean","Separators");
run("Fill Holes");
rename("Myelin_outlines");
selectImage(name);
run("Analyze Particles...", "  circularity=0-Infinity show=Masks exclude");
rename("Inner_to_count");
selectWindow("Myelin_outlines");
run("Options...", "iterations=20 count=1 pad do=Open");
run("Watershed");
run("BinaryReconstruct ", "mask=Myelin_outlines seed=Inner_to_count create white");
rename("Outlines_to_count");
run("Options...", "iterations=1 count=1 pad do=Close");
run("Analyze Particles...", "  circularity=0-Infinity show=Nothing add");
run("Merge Channels...", "c4="+imageName+" c6="+name+" create keep");
rename("Composite_in_out");
roiManager("Show All");
roiManager("Save", dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+"_RoiSet_OUT.zip");
setTool("freehand");
close("Outlines_to_count");
close("Separators");
close("Myelin");
close("Myelin_inverted");
close("Myelin_clean");
close("Myelin_outlines");
waitForUser(titleWFU, msgWFU);
close("Composite_in_out");
roiManager("Save", dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+"_RoiSet_OUT.zip");


roiManager("Deselect");
roiManager("Combine");
run("Create Mask");
rename("Outlines_to_count");
selectWindow("ROI Manager");
run("Close");
run("BinaryReconstruct ", "mask=Inner_to_count seed=Outlines_to_count create white");
rename("Inner_to_count_corrected");
imageCalculator("XOR create", "Inner_to_count_corrected","Outlines_to_count");
rename("Myelin_to_count");
close("Inner_to_count");
//close("Inner_to_count_corrected");
//close("Outlines_to_count");
open(dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+objectTag);
rename(objectTag);
run("Duplicate...", "title=axon_class");
rename("axon_class");
setThreshold(1, 2);
setOption("BlackBackground", false);
run("Convert to Mask");
run("Options...", "iterations=5 count=1 pad do=Close");
run("Fill Holes");
imageCalculator("AND create", "axon_class","Outlines_to_count");
rename("myel_axon_class");
run("Analyze Particles...", "  circularity=0.00-Infinity add");

//convex hull
roiN=roiManager("count");
for (i=0; i<roiN; i++) {
	roiManager("Select", i);
	run("Convex Hull");
	roiManager("Update");
}
roiManager("Deselect");
roiManager("Combine");
run("Create Mask");
rename("Axons_ConvexHull");
selectWindow("ROI Manager");
run("Close");
imageCalculator("AND create", "Inner_to_count_corrected","Axons_ConvexHull");
rename("Axons_ConvexHull_Corrected");
run("Analyze Particles...", "  circularity=0.00-Infinity add");
close("Axons_ConvexHull");
close("Axons_ConvexHull_Corrected");

selectWindow(objectTag);
run("Duplicate...", "title=reject_class");
setThreshold(3, 4);
run("Convert to Mask");
run("Options...", "iterations=5 count=1 pad do=Close");
imageCalculator("AND create", "reject_class","Outlines_to_count");
rename("myel_reject_class");
run("Fill Holes");
run("Merge Channels...", "c3=myel_reject_class c6=Myelin_to_count c4="+imageName+" create keep");
Stack.setChannel(1);
roiManager("Show All");
setTool("freehand");
//close images
close("myel_reject_class");
close("reject_class");
close(objectTag);
close("myel_axon_class");
close("axon_class");
close("Myelin_to_count");
close("Outlines_to_count");
close("Inner_to_count_corrected");
close(imageName);
close(name);

//Edition, save and close all
waitForUser(titleWFU, msgWFU);
roiManager("Save", dir+File.separator+substring(imageName, 0, indexOf(imageName, "."))+"_RoiSet_AXON.zip");
run("Close All");
if (isOpen("ROI Manager")) {
	selectWindow("ROI Manager");
	run("Close");
}
if (isOpen("Channels")) {
	selectWindow("Channels");
	run("Close");
}