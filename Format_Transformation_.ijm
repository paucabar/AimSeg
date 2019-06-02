//Browse a directory and a postfix to add to the work folder
#@ File(label="Directory", style="directory") dir
#@ String (label="Postfix") postfix

original=File.getName(dir);
list=getFileList(dir);
output=File.getParent(dir)+File.separator+original+"_"+postfix;
File.makeDirectory(output);

setBatchMode(true);
print("Macro starts");

//Convert the image datset into tif files with a bit depth of 8-bit
for (i=0; i<list.length; i++) {
	print(list[i], i+1 + "/" + list.length);
	open(dir+File.separator+list[i]);
	name=File.nameWithoutExtension;
	run("8-bit");
	saveAs("tif", output+File.separator+name);
	close(name+".tif");
}

setBatchMode(false);
print("Macro ends");