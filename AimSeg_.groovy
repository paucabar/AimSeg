#@ File(label="Image File", style="open") imageFile
#@ Integer (label="Myelin Probability Channel", value=1, max=3, min=1, style="listBox") probChannel
#@ String (label="Object Prediction Threshold", choices={"Below", "Above"}, value="Above", style="radioButtonHorizontal") objThr
#@ Integer (label="Object Prediction Label", value=2, max=10, min=1, style="listBox") objLabel
#@ boolean (label="Automated", value=true, persist=false) automated
#@ UpdateService updateService
#@ UIService ui
#@ LogService logService
#@ StatusService statusService


import java.io.File
import ij.IJ
import ij.Prefs
import ij.io.Opener
import ij.ImagePlus
import groovy.io.FileType
import ij.plugin.Duplicator
import ij.process.ImageProcessor
import ij.process.ByteProcessor
import ij.process.FloodFiller
import ij.plugin.filter.ParticleAnalyzer
import ij.measure.ResultsTable
import ij.measure.Measurements
import ij.plugin.ImageCalculator
import ij.gui.WaitForUserDialog
import net.imglib2.img.display.imagej.ImageJFunctions
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import ij.gui.Overlay
import ij.plugin.filter.ThresholdToSelection
import java.awt.Color
import ij.plugin.Duplicator
import inra.ijpb.watershed.MarkerControlledWatershedTransform2D
import ij.plugin.Commands
import ij.process.FloatPolygon
import ij.gui.PolygonRoi

/*
import ij.plugin.RGBStackMerge
import ij.plugin.RGBStackConverter
import ij.CompositeImage
import ij.process.LUT
*/

boolean isUpdateSiteActive (updateSite) {
	boolean checkUpdate = true
	if (! updateService.getUpdateSite(updateSite).isActive()) {
    	ui.showDialog "Please activate the $updateSite update site"
    	checkUpdate = false
	}
	return checkUpdate
}

def installMacro () {
	String toolsetsPath = IJ.getDir("macros") + "toolsets"
	String ijmPath = IJ.addSeparator(toolsetsPath)+"AimSeg_Macros.ijm"
	IJ.run("Install...", "install=[${->ijmPath}]")
}

ImagePlus importImage (File inputFile, String datasetName, String axisOrder) {
	String imagePath = inputFile.getAbsolutePath()
	if (!imagePath.endsWith(".h5")) {		
		def opener = new Opener()
		String extension = imagePath[imagePath.lastIndexOf('.')+1..-1]
		println "Importing $extension file"
		result = opener.openImage(imagePath)
	} else {
		println "Importing h5 file"
		def imp = new Hdf5DataSetReader<>(
		                imagePath,
		                datasetName,
		                axisOrder.toLowerCase(),
		                logService,
		                statusService).read()
		result = ImageJFunctions.wrap(imp, "Some title here")
	}
	return result
}

// Implements the Erode, Dilate, Open and Close commands in the Process/Binary submenu
def run (ImageProcessor ip, String arg, int iter, int cnt) {
    int fg = Prefs.blackBackground ? 255 : 0
    foreground = ip.isInvertedLut() ? 255-fg : fg
    background = 255 - foreground
    ip.setSnapshotCopyMode(true)

	if (arg.equals("erode") || arg.equals("dilate")) {
        doIterations((ByteProcessor)ip, arg, iter, cnt)
	} else if (arg.equals("open")) {
        doIterations(ip, "erode", iter, cnt)
        doIterations(ip, "dilate", iter, cnt)
    } else if (arg.equals("close")) {
        doIterations(ip, "dilate", iter, cnt)
        doIterations(ip, "erode", iter, cnt)
    }
    ip.setSnapshotCopyMode(false)
    ip.setBinaryThreshold()
}

def doIterations (ImageProcessor ip, String mode, int iterations, int count) {
    for (int i=0; i<iterations; i++) {
        if (Thread.currentThread().isInterrupted()) return
        if (IJ.escapePressed()) {
            boolean escapePressed = true
            ip.reset()
            return
        }
        if (mode.equals("erode"))
            ((ByteProcessor)ip).erode(count, background)
        else
            ((ByteProcessor)ip).dilate(count, background)
    }
}

// Binary fill by Gabriel Landini, G.Landini at bham.ac.uk
// 21/May/2008
def fill (ImageProcessor ip) {
    int fg = Prefs.blackBackground ? 255 : 0
    int foreground = ip.isInvertedLut() ? 255-fg : fg
    int background = 255 - foreground
    ip.setSnapshotCopyMode(true)
    
    int width = ip.getWidth()
    int height = ip.getHeight()
    def FloodFiller ff = new FloodFiller(ip)
    ip.setColor(127)
    for (int y=0; y<height; y++) {
        if (ip.getPixel(0,y)==background) ff.fill(0, y)
        if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y)
    }
    for (int x=0; x<width; x++) {
        if (ip.getPixel(x,0)==background) ff.fill(x, 0)
        if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1)
    }
    byte[] pixels = (byte[])ip.getPixels()
    int n = width*height
    for (int i=0; i<n; i++) {
    if (pixels[i]==127)
        pixels[i] = (byte)background
    else
        pixels[i] = (byte)foreground
    }
}

ImagePlus analyzeParticles (ImagePlus imp, int options, int measurements, double minSize, double maxSize, double minCirc, double maxCirc) {
	def rt = new ResultsTable()
	def pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize, minCirc, maxCirc)
	ImageProcessor ip = imp.getProcessor()
	ip.setBinaryThreshold()
	pa.setHideOutputImage(true)
	pa.analyze(imp, ip)
	ImagePlus impOutput = pa.getOutputImage()
	if (impOutput.isInvertedLut()) {
		IJ.run(impOutput, "Grays", "")
	}
	return impOutput
}

def cleanRoiSet (ImagePlus imp, RoiManager rm) {
	def roiDiscard = []
	int count = rm.getCount()
	rm.getRoisAsArray().eachWithIndex { roi, index ->
	    if (roi.getGroup() != 2) {
		    roiDiscard.add(index)
	    } else {
	    	roi.setGroup(0)
	    	roi.setStrokeWidth(0)
	    }
	}
	if (roiDiscard.size > 0) {
		println "Discard ROIs $roiDiscard"
		int[] roiDiscardInt = roiDiscard as int[]
		rm.setSelectedIndexes(roiDiscardInt)
		rm.runCommand(imp,"Delete")
	}
}

ImagePlus createRoiMask (ImagePlus imp, RoiManager rm) {
	IJ.run(imp, "Select None", "");
	rm.deselect()
	rm.runCommand(imp,"Combine")
	ByteProcessor mask = imp.createRoiMask()
	ImagePlus impMask = new ImagePlus("Mask", mask)
	return impMask
}

ImagePlus setMaskOverlay (ImagePlus imp, ImagePlus impMask, int alpha) {
	ImageProcessor ip = impMask.getProcessor()
	ip.setThreshold (255, 255)
	def tts = new ThresholdToSelection()
	Roi roi = tts.convert(ip)
	
	def ovl = new Overlay(roi)
	ovl.setFillColor(new Color(255,0,255,alpha))
	
	imp.setOverlay(ovl)
	ImagePlus impFlatten = imp.flatten()
	
	ovl.clear()
	imp.setOverlay(ovl)
	
	return impFlatten
}

ImagePlus runMarkerControlledWatershed (ImageProcessor input, ImageProcessor labels, ImageProcessor mask, int connectivity) {
	def mcwt = new MarkerControlledWatershedTransform2D (input, labels, mask, connectivity)
	ImageProcessor result = mcwt.applyWithPriorityQueue()
	ImagePlus impResult = new ImagePlus("Out_to_count", result)
	return impResult
}

// binary reconstruct by Landini
// reconstruction on seed image
def runBinaryReconstruct (ImagePlus imp1, ImagePlus imp2) {
	def BinaryReconstruct_ br = new BinaryReconstruct_()
	Object[] result = br.exec(imp1, imp2, null, false, true, false )
	//parameters above are: mask ImagePlus, seed ImagePlus, name, create new image, white particles, connect4
	if (null != result) {
	  String name = (String) result[0]
	  ImagePlus recons = (ImagePlus) result[1]
	}
}

def transferProperties (Roi roi1, Roi roi2) {
	if (roi1==null || roi2==null)
		return
		roi2.setStrokeColor(roi1.getStrokeColor())
	if (roi1.getStroke()!=null)
		roi2.setStroke(roi1.getStroke())
		roi2.setDrawOffset(roi1.getDrawOffset())
	if (roi1.getGroup()!=null)
		roi2.setGroup(roi1.getGroup())
}

def convexHull (ImagePlus imp, RoiManager rm) {
	int roiCount = rm.getCount()
	println "$roiCount selected ROIs"
	for (i in 0..roiCount-1) {
		rm.select(0)
		Roi roi = imp.getRoi()
		FloatPolygon p = roi.getFloatConvexHull()
			if (p!=null) {
				Roi roi2 = new PolygonRoi(p, Roi.POLYGON)
				transferProperties(roi, roi2)
				rm.addRoi(roi2)
				rm.select(0)
				rm.runCommand(imp,"Delete")
			}
	}
}



//////////////
// START
//////////////

// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik")
boolean checkMorphology = isUpdateSiteActive("Morphology")
boolean checkMorphoLibJ = isUpdateSiteActive("IJPB-plugins")

// exit if any update site is missing
if (!checkIlastik || !checkMorphoLibJ || !checkMorphology) {
	return
}

// setup
installMacro()

bb = Prefs.blackBackground
if (!bb) {
	Prefs.blackBackground=true
}

pe = Prefs.padEdges
if (!pe) {
	Prefs.padEdges = true
}

// import EM image
imp = importImage(imageFile, "/data", "tzyxc")
imp.show()

// import the corresponding probability map and object prediction
// IMPORTANT: must be in the parent folder
File parentPath = imageFile.getParentFile()
def fileList = []
parentPath.eachFile(FileType.FILES) {
	fileList << it.name
}
String impNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))
String probNameWithoutExtension = impNameWithoutExtension+"_Probabilities"
String objNameWithoutExtension = impNameWithoutExtension+"_Object Predictions"
String probFilename = fileList.find {element -> element.contains(probNameWithoutExtension)}
String objFilename = fileList.find {element -> element.contains(objNameWithoutExtension)}
File probFile = new File (parentPath, probFilename)
File objFile = new File (parentPath, objFilename)
impProb = importImage(probFile, "/exported_data", "yxc")
impObj = importImage(objFile, "/exported_data", "yxc")
IJ.run(impObj, "glasbey inverted", "")



//////////////
// STAGE 1
//////////////

// timing
int t0 = System.currentTimeMillis()

// create myelin mask
impProb.setPosition(probChannel)
ImageProcessor ipProb = impProb.getProcessor()
ipProb.setThreshold (0.2, 1.0)
ImageProcessor ipMyelinMask = ipProb.createMask() // image processor
ImagePlus impMyelinMask = new ImagePlus("Myelin Mask", ipMyelinMask) // image processor to image plus
//impMyelinMask.show()

// duplicate and invert myelin mask
ImageProcessor ipMyelinMaskInverted = ipMyelinMask.duplicate()
ipMyelinMaskInverted.invert()
ImagePlus impMyelinMaskInverted = new ImagePlus("Myelin Inverted Mask", ipMyelinMaskInverted) // image processor to image plus
//impMyelinMaskInverted.show()

// exclude edges
int options_exclude_edges = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
int measurements_area = Measurements.AREA
ImagePlus impNoEdges = analyzeParticles(impMyelinMaskInverted, options_exclude_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
//impNoEdges.show()

// close and fill holes
Prefs.padEdges = false
run(impNoEdges.getProcessor(), "close", 2, 1)
Prefs.padEdges = true
fill(impNoEdges.getProcessor())

// image calculator XOR
def ic = new ImageCalculator()
ImagePlus impXOR = ic.run(impMyelinMaskInverted, impNoEdges, "XOR create")
//impXOR.show()

// get axons on edges
int options_with_edges = ParticleAnalyzer.SHOW_MASKS
ImagePlus impEdges = analyzeParticles(impXOR, options_with_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0.3, 1)
//impEdges.show()

// image calculator OR
ImagePlus impOR = ic.run(impNoEdges, impEdges, "OR create")
//impOR.show()

// get inner masks
int inMinArea = 10000 // min size to filter IN particles
int options_add_manager = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.ADD_TO_MANAGER
ImagePlus innerMasks = analyzeParticles(impOR, options_add_manager, measurements_area, inMinArea, Double.POSITIVE_INFINITY, 0.4, 1)
//innerMasks.show()

// RoiManager set selected objects as group 2 (red ROIs)
RoiManager rm = RoiManager.getInstance()
int roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
}

// get other masks
ImagePlus allMasks = ic.run(innerMasks, impMyelinMaskInverted, "OR create")
ImagePlus otherMasks = ic.run(innerMasks, allMasks, "XOR create")
ImagePlus otherMasksSizeFilter = analyzeParticles(otherMasks, options_add_manager, measurements_area, 3000, 500000, 0, 1)

// RoiManager set other objects as group 1 (blue ROIs)
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (index > roiCount -1) {
	    roi.setGroup(1)
	    roi.setStrokeWidth(5)
    }
}

// wait for user
def wfu = new WaitForUserDialog("Edit ROIs", "If necessary, use the \"ROI Manager\" to edit\nthe output. Click \"OK\" once you finish")
if (!automated) wfu.show()


// discard group 1 ROIs and set group 2 ROIs as 0
// set stroke width as 0
cleanRoiSet(imp, rm)

// save ROI set IN
rm.deselect()
String parentPathS = imageFile.getParentFile()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
println "Save RoiSet_IN"

// timing
int t1 = System.currentTimeMillis()
println t1-t0

//////////////
// PRE-STAGE 2
//////////////

// create IN final mask
ImagePlus maskIn = createRoiMask(imp, rm)
rm.runCommand(imp,"Delete")
//rm.close()
imp.hide()

/*
// merge channels to display
ImagePlus[] impMergeIN = [imp, maskIn]
def rgbSMerge = new RGBStackMerge()
ImagePlus maskOverlayIN = rgbSMerge.mergeChannels(impMergeIN, true)
//println maskOverlayIN.getClass()

// set composite LUTs
luts = maskOverlayIN.getLuts()
luts[0] = LUT.createLutFromColor(Color.GRAY)
luts[1] = LUT.createLutFromColor(Color.MAGENTA)
maskOverlayIN.setLuts(luts)
maskOverlayIN.updateAndDraw()

// convert to RGB
def rgbSConverter = new RGBStackConverter()
rgbSConverter.convertToRGB(maskOverlayIN)
maskOverlayIN.show()
*/

ImagePlus maskOverlayIN = setMaskOverlay(imp, maskIn, 127)
maskOverlayIN.show()

//////////////
// STAGE 2
//////////////

// timing
t0 = System.currentTimeMillis()

// duplicate IN final mask
def dup = new Duplicator()
ImagePlus impVoronoi = dup.run(maskIn, 1, 1, 1, 1, 1, 1);

// get voronoi masks
IJ.run(impVoronoi, "Voronoi", "")
impVoronoi.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE)
impVoronoi.setProcessor(impVoronoi.getProcessor().createMask())
impVoronoi.getProcessor().invert()
run (impVoronoi.getProcessor(), "erode", 2, 1)

// get initial OUT masks
ImagePlus myelinFirst = ic.run(impMyelinMask, maskIn, "AND create")
ImagePlus myelinClean = ic.run(myelinFirst, impMyelinMask, "XOR create")
ImagePlus myelinOutlines = ic.run(myelinClean, impVoronoi, "AND create")
fill(myelinOutlines.getProcessor())
ImagePlus impInToCount = analyzeParticles(maskIn, options_exclude_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
run (myelinOutlines.getProcessor(), "open", 20, 1)
IJ.run(myelinOutlines, "Watershed", "")
// int options_count_masks = ParticleAnalyzer.SHOW_ROI_MASKS
// ImagePlus impInToCountLabels = analyzeParticles(impInToCount, options_count_masks, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
// ImagePlus myelinOutlines2 = runMarkerControlledWatershed(myelinOutlines.getProcessor(), impInToCountLabels.getProcessor(), myelinOutlines.getProcessor(), 8)
//myelinOutlines2.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE)
//myelinOutlines2.setProcessor(myelinOutlines2.getProcessor().createMask())
ImagePlus impMyelinOutlines2 = impInToCount.duplicate()
runBinaryReconstruct(myelinOutlines, impMyelinOutlines2)
run (impMyelinOutlines2.getProcessor(), "close", 1, 1)
ImagePlus impOutMasks = analyzeParticles(impMyelinOutlines2, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)

// RoiManager set selected objects as group 2 (red ROIs)
roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
}

// wait for user
if (!automated) wfu.show()

// discard group 1 ROIs and set group 2 ROIs as 0
// set stroke width as 0
cleanRoiSet(maskOverlayIN, rm)

// save ROI set OUT
rm.deselect()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")
println "Save RoiSet_OUT"

// timing
t1 = System.currentTimeMillis()
println t1-t0

//////////////
// PRE-STAGE 3
//////////////

// create OUT final mask
ImagePlus maskOut = createRoiMask(maskOverlayIN, rm)
rm.runCommand(maskOverlayIN,"Delete")
//rm.close()
maskOverlayIN.hide()

// get myelin to count
ImagePlus impInToCountCor = maskOut.duplicate()
runBinaryReconstruct(impInToCount, impInToCountCor)
//ImagePlus maskOutLabels = analyzeParticles(maskOut, options_count_masks, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
//ImagePlus impInToCountCor= runMarkerControlledWatershed(impInToCount.getProcessor(), maskOutLabels.getProcessor(), impInToCount.getProcessor(), 8)
//impInToCountCor.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE)
//impInToCountCor.setProcessor(impInToCountCor.getProcessor().createMask())
ImagePlus myelinToCount = ic.run(impInToCountCor, maskOut, "XOR create")

/*
// merge channels to display
ImagePlus[] impMergeMyelin = [imp, myelinToCount]
ImagePlus maskOverlayMyelin = rgbSMerge.mergeChannels(impMergeMyelin, true)
//println maskOverlayIN.getClass()

// set composite LUTs
lutsM = maskOverlayMyelin.getLuts()
lutsM[0] = LUT.createLutFromColor(Color.GRAY)
lutsM[1] = LUT.createLutFromColor(Color.MAGENTA)
maskOverlayMyelin.setLuts(lutsM)
maskOverlayMyelin.updateAndDraw()

// convert to RGB
rgbSConverter.convertToRGB(maskOverlayMyelin)
maskOverlayMyelin.show()
*/

ImagePlus maskOverlayMyelin = setMaskOverlay(imp, myelinToCount, 127)
maskOverlayMyelin.show()

//////////////
// STAGE 3
//////////////

// timing
t0 = System.currentTimeMillis()

// segment object predictions: select class
ImagePlus impAxonMasks = dup.run(impObj, 1, 1, 1, 1, 1, 1);
impAxonMasks.getProcessor().setThreshold(1, 2, ImageProcessor.NO_LUT_UPDATE)
impAxonMasks.setProcessor(impAxonMasks.getProcessor().createMask())
run (impAxonMasks.getProcessor(), "close", 5, 1)
fill(impAxonMasks.getProcessor())
ImagePlus impAxonMasksFiltered = ic.run(impAxonMasks, maskOut, "AND create")
ImagePlus dupAxonMasksFiltered = analyzeParticles(impAxonMasksFiltered, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)

// get convex hull from ROIs
convexHull(maskOverlayMyelin, rm)

// create convex hull mask
ImagePlus convexHullMask = createRoiMask(maskOverlayMyelin, rm)
rm.runCommand(maskOverlayMyelin,"Delete")

// correct convex hull and get ROIs
ImagePlus convexHullMaskCorrected = ic.run(convexHullMask, impInToCount, "AND create")
convexHullMaskCorrected = analyzeParticles(convexHullMaskCorrected, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)

// RoiManager set selected objects as group 2 (red ROIs)
// get ROIs convex hull
roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
}

// segment object predictions: reject class
if (objThr == "Above") {
	minObj=objLabel+1
	maxObj=255
} else {
	minObj=0
	maxObj=objLabel-1
}
ImagePlus impRejectMasks = dup.run(impObj, 1, 1, 1, 1, 1, 1);
impRejectMasks.getProcessor().setThreshold(minObj, maxObj, ImageProcessor.NO_LUT_UPDATE)
impRejectMasks.setProcessor(impRejectMasks.getProcessor().createMask())
run (impRejectMasks.getProcessor(), "close", 5, 1)
ImagePlus impRejectMasksFiltered = ic.run(impRejectMasks, maskOut, "AND create")
fill(impRejectMasksFiltered.getProcessor())
impRejectMasksFiltered = analyzeParticles(impRejectMasksFiltered, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)

// RoiManager set other objects as group 1 (blue ROIs)
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (index > roiCount -1) {
	    roi.setGroup(1)
	    roi.setStrokeWidth(5)
    }
}

// wait for user
if (!automated) wfu.show()

// discard group 1 ROIs and set group 2 ROIs as 0
// set stroke width as 0
cleanRoiSet(maskOverlayMyelin, rm)

// save ROI set AXON
rm.deselect()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")
println "Save RoiSet_AXON"

// timing
t1 = System.currentTimeMillis()
println t1-t0

//////////////
// RESET
//////////////

// close all
rm.deselect()
rm.runCommand(maskOverlayMyelin,"Delete")
rm.close()
Commands cmd = new Commands()
cmd.closeAll()

// reset Prefs.padEdges
Prefs.padEdges = pe
Prefs.blackBackground = bb
println "Processing finished"
return