//script parameters
#@ File(label="Directory", style="directory") dir
#@ File(label="Pixel Classification", description="Enter an ilastik project (ilp) file", style="extensions:ilp") projectPixel
#@ File(label="Object Classification", description="Enter an ilastik project (ilp) file", style="extensions:ilp") projectObject

//check if the folder contains the h5 files
dirName=File.getName(dir);
list=getFileList(dir);
count=0;
for (i=0; i<list.length; i++) {
	if (endsWith(list[i], ".h5")==true) {
		count++;
	}
}

//error message
if (count==0) {
	exit("No h5 files found in "+dirName);
}