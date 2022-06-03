#@ File(label="Image File", style="open") imageFile

import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import groovy.io.FileType
import ij.gui.WaitForUserDialog

// setup
installMacro()
imp = importImage(imageFile, "/data", "tzyxc")

// import the corresponding probability map
// IMPORTANT: must be in the parent folder
File parentPath = imageFile.getParentFile()
def fileList = []
parentPath.eachFile(FileType.FILES) {
	fileList << it.name
}
String probNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))+"_Probabilities"
String probFilename = fileList.find {element -> element.contains(probNameWithoutExtension)}
File probFile = new File (parentPath, probFilename)
impProb = importImage(probFile, "/exported_data", "yxc")
//impProb.hide()

// wait for user
def wfu = new WaitForUserDialog("Title", "I'm waiting")
wfu.show()






def installMacro(){
	String toolsetsPath = IJ.getDir("macros") + "toolsets"
	String ijmPath = IJ.addSeparator(toolsetsPath)+"Toggle_ROI_color.ijm"
	IJ.run("Install...", "install=[${->ijmPath}]")
}

def importImage(File inputFile, String datasetname, String axisorder){
	String imagePath = inputFile.getAbsolutePath()
	if (!imagePath.endsWith(".h5")) {		
		def opener = new Opener()
		String extension = imagePath[imagePath.lastIndexOf('.')+1..-1]
		println "Importing $extension file"
		implus = opener.openImage(imagePath)
		implus.show()
	} else {
		args = "select=[" + imagePath + "] datasetname=$datasetname axisorder=$axisorder"
		println "Importing h5 file"
		IJ.run("Import HDF5", args)
		implus = IJ.getImage()
	}
	return implus
}