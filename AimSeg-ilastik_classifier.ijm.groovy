#@ File(label="Image File", style="open") imageFile
#@ Integer (label="Myelin Probability Channel", value=1, max=3, min=1, style="listBox") probChannel
#@ String (label="Object Prediction Threshold", choices={"Below", "Above"}, value="Above", style="radioButtonHorizontal") objThr
#@ Integer (label="Object Prediction Label", value=2, max=10, min=1, style="listBox") objLabel
#@ UpdateService updateService
#@ UIService ui
#@ LogService logService
#@ StatusService statusService

import ij.IJ
import ij.Prefs
import ij.io.Opener
import ij.ImagePlus
import groovy.io.FileType
import ij.plugin.Duplicator
import ij.process.ImageProcessor
import ij.plugin.filter.ParticleAnalyzer
import ij.measure.ResultsTable
import ij.measure.Measurements
import ij.plugin.ImageCalculator
import ij.gui.WaitForUserDialog
import net.imglib2.img.display.imagej.ImageJFunctions
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader

// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik");

// setup
installMacro()
//Prefs.blackBackground=true

// import EM image
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
//impProb.hide()
impObj = importImage(objFile, "/exported_data", "yxc")
IJ.run(impObj, "glasbey inverted", "")
//impObj.hide()



//////////////
// STAGE 1
//////////////

// create myelin mask
impProb.setPosition(probChannel)
def ipProb = impProb.getProcessor()
ipProb.setThreshold (0.2, 1.0)
def ipMyelinMask = ipProb.createMask() // image processor
def impMyelinMask = new ImagePlus("Myelin Mask", ipMyelinMask) // image plus
impMyelinMask.show()

// duplicate and invert myelin mask
def ipMyelinMaskInverted = ipMyelinMask.duplicate()
ipMyelinMaskInverted.invert()
def impMyelinMaskInverted = new ImagePlus("Myelin Inverted Mask", ipMyelinMaskInverted)
impMyelinMaskInverted.show()

// exclude edges and big objects
Integer options = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
Integer measurements = Measurements.AREA
def rt = new ResultsTable();
def pa = new ParticleAnalyzer(options, measurements, rt, 10000, 999999)
pa.setHideOutputImage(true)
pa.analyze(impMyelinMaskInverted)
def impNoEdges = pa.getOutputImage()

// close and fill holes
IJ.run(impNoEdges, "Options...", "iterations=2 count=1 black do=Open");
IJ.run(impNoEdges, "Options...", "iterations=1 count=1 do=[Fill Holes]");

// image calculator
def ic = new ImageCalculator()
def impXOR = ic.run(impMyelinMaskInverted, impNoEdges, "XOR create")
impXOR.show()

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

def importImage(File inputFile, String datasetName, String axisOrder){
	String imagePath = inputFile.getAbsolutePath()
	if (!imagePath.endsWith(".h5")) {		
		def opener = new Opener()
		String extension = imagePath[imagePath.lastIndexOf('.')+1..-1]
		println "Importing $extension file"
		implus = opener.openImage(imagePath)
		implus.show()
	} else {
		println "Importing h5 file"
		def imgPlus = new Hdf5DataSetReader<>(
		                imagePath,
		                datasetName,
		                axisOrder.toLowerCase(),
		                logService,
		                statusService).read();
		implus = ImageJFunctions.wrap(imgPlus, "Some title here")
	}
	return implus
}