//script parameters
#@ File(label="Directory", style="directory") dir
#@ String (visibility=MESSAGE, value="Enter the extension of the files to be transformed") msg1
#@ String (label="Extension") extension
#@ String (visibility=MESSAGE, value="Enter a postfix to tag the output folder") msg
#@ String (label="Postfix") postfix

print("\\Clear");
original=File.getName(dir);
list=getFileList(dir);
output=File.getParent(dir)+File.separator+original+"_"+postfix;

//check if the folder contains the specified files
count=0;
for (i=0; i<list.length; i++) {
	if (endsWith(list[i], extension)==true) {
		count++;
	}
}

//error message
if (count==0) {
	exit("No "+extension+" files found in "+original);
}

//create the output folder
print("Macro starts");
File.makeDirectory(output);

//convert the image datset into tif files with a bit depth of 8-bit
setBatchMode(true);
for (i=0; i<list.length; i++) {
	print(list[i], i+1 + "/" + list.length);
	open(dir+File.separator+list[i]);
	name=File.nameWithoutExtension;
	input=File.name;
	run("8-bit");
	run("Export HDF5", "select=["+output+File.separator+name+".h5] exportpath=["+output+File.separator+name+".h5] datasetname=data compressionlevel=0 input="+input);
	close(input);
}
setBatchMode(false);
print("Macro ends");