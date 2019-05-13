dir=getDirectory("Choose a Directory");
list=getFileList(dir);
output=File.getParent(dir)+File.separator+"ilastik_suited_dataset";
File.makeDirectory(output);

setBatchMode(true);
print("Macro starts");

//Convert the image datset into TIF files
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