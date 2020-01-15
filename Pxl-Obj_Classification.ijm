//script parameters
#@ File(label="Directory", style="directory") dir
#@ File(label="Pixel Classification", description="Enter an ilastik project (ilp) file", style="extensions:ilp") projectPixel
#@ File(label="Object Classification", description="Enter an ilastik project (ilp) file", style="extensions:ilp") projectObject

//check if the folder contains h5 files
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

for (i=0; i<list.length; i++) {
	if (endsWith(list[i], ".h5")) {
		fileNameWithFormat=File.getName(list[i]);
		indexFormat=indexOf(fileNameWithFormat, ".h5");
		fileName=substring(fileNameWithFormat, 0, indexFormat);
		run("Import HDF5", "select=["+dir+File.separator+list[i]+"] datasetname=[/data] axisorder=tzyxc");
		rename(fileName);
		run("Run Pixel Classification Prediction", "projectfilename=["+projectPixel+"] inputimage=["+fileName+"] pixelclassificationtype=Probabilities");
		rename("Probabilities");
		run("Export HDF5", "select=[Probabilities] exportpath=["+dir+File.separator+fileName+"_Probabilities.h5] datasetname=data compressionlevel=0 input=Probabilities");
	}
}


