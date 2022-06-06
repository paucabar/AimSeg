#@ File(label="Image File", style="open") imageFile
#@ UpdateService updateService
#@ UIService ui

import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import groovy.io.FileType
import ij.plugin.Duplicator
import ij.process.ImageProcessor
import ij.plugin.filter.ParticleAnalyzer
import ij.measure.ResultsTable
import ij.measure.Measurements
import ij.gui.WaitForUserDialog

// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik");

// setup
installMacro()
imp = importImage(imageFile, "/data", "tzyxc")

// import the corresponding probability map and object prediction
// IMPORTANT: must be in the parent folder
File parentPath = imageFile.getParentFile()
def fileList = []
parentPath.eachFile(FileType.FILES) {
	fileList << it.name
}
String probNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))+"_Probabilities"
String objNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))+"_Object Predictions"
String probFilename = fileList.find {element -> element.contains(probNameWithoutExtension)}
String objFilename = fileList.find {element -> element.contains(objNameWithoutExtension)}
File probFile = new File (parentPath, probFilename)
File objFile = new File (parentPath, objFilename)
impProb = importImage(probFile, "/exported_data", "yxc")
impProb.hide()
impObj = importImage(objFile, "/exported_data", "yxc")
IJ.run(impObj, "glasbey inverted", "")
impObj.hide()

//////////////
// STAGE 1
//////////////

// create myelin mask
impProb.setPosition(1)
def ipProb = impProb.getProcessor()
ipProb.setThreshold (0.2, 1.0)
def ipMyelinMask = ipProb.createMask() // image processor
def impMyelinMask = new ImagePlus("Myelin Mask", ipMyelinMask) // image plus
//impMyelinMask.show()

// duplicate and invert mask
def dup = new Duplicator()
def ipMyelinMaskInverted = ipMyelinMask.duplicate()
ipMyelinMaskInverted.invert()
def impMyelinMaskInverted = new ImagePlus("Myelin Inverted Mask", ipMyelinMaskInverted)

// exclude edges
Integer options = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
Integer measurements = Measurements.AREA
def rt = new ResultsTable();
def pa = new ParticleAnalyzer(options, measurements, rt, 0, 999999)
pa.analyze(impMyelinMaskInverted)
def impNoEdges = pa.getOutputImage()
//impNoEdges.show()

// wait for user
def wfu = new WaitForUserDialog("Title", "I'm waiting")
wfu.show()




def isUpdateSiteActive (updateSite) {
	checkUpdate = true
	if (! updateService.getUpdateSite(updateSite).isActive()) {
    	ui.showDialog "Please activate the $updateSite update site"
    	checkUpdate = false
	}
	return checkUpdate
}

def installMacro(){
	String toolsetsPath = IJ.getDir("macros") + "toolsets"
	String ijmPath = IJ.addSeparator(toolsetsPath)+"AimSeg_Macros.ijm"
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