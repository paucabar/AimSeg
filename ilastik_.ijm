//script parameters
#@ File(label="Directory", style="directory") dir
#@ String (label="Extension", description="Enter the extension of the files to be transformed. Image extensions: tif, png, jpg, others...") extension
#@ String (label="Normalize", choices={"Yes", "No"}, style="radioButtonHorizontal") normalize
#@ String (label="Postfix", description="Enter a postfix to tag the output folder") postfix
#@ String (label=" ", value="<html><img src=\"https://live.staticflickr.com/65535/48557333566_d2a51be746_o.png\"></html>", visibility=MESSAGE, persist=false) logo
#@ String (label=" ", value="<html><font size=2><b>Neuromolecular Biology Laboratory</b><br>ERI BIOTECMED - Universitat de València (Spain)</font></html>", visibility=MESSAGE, persist=false) message

print("\\Clear");
original=File.getName(dir);
list=getFileList(dir);
output=File.getParent(dir)+File.separator+original+"_"+postfix;

//check if the folder contains the specified files
count=0;
for (i=0; i<list.length; i++) {
	if (endsWith(list[i], extension) == true) {
		count++;
	}
}

//error message
if (count==0) {
	exit("No "+extension+" files found in "+original);
}

//create the output folder
File.makeDirectory(output);

//normalize and export data
setBatchMode(true);
for (i=0; i<list.length; i++) {
	print(list[i], i+1 + "/" + list.length);
	open(dir+File.separator+list[i]);
	name=File.nameWithoutExtension;
	if (normalize == "Yes") {
		run("32-bit");
		run("Enhance Contrast...", "saturated=0.1 normalize");
	}
	saveAs("tif", output+File.separator+name);
	close(name+".tif");
}
setBatchMode(false);
print("Macro ends");