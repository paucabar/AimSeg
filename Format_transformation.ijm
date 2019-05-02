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

//Create a file containing the scale info
open(dir+File.separator+list[0]);
getPixelSize(unit, pixelWidth, pixelHeight);
close();
title1 = "pixel_size";
title2 = "["+title1+"]";
f = title2;
run("Table...", "name="+title2+" width=500 height=500");
print(f, "unit\t" + unit);
print(f, "pixelWidth\t" + pixelWidth);
print(f, "pixelHeight\t" + pixelHeight);
saveAs("txt", output+File.separator+title1);
selectWindow(title1);
run("Close");

setBatchMode(false);
print("Macro ends");