#@ File(label="Image File", style="open") imageFile
#@ Integer (label="Myelin Probability Channel", value=1, max=3, min=1, style="listBox") probChannel
#@ String (label="Object Prediction Threshold", choices={"Below", "Above"}, value="Above", style="radioButtonHorizontal") objThr
#@ Integer (label="Object Prediction Label", value=2, max=10, min=1, style="listBox") objLabel
#@ boolean (label="Automated", value=true, persist=false) automated
#@ boolean (label="Add axons", value=false, persist=false) addAxons
#@ UpdateService updateService
#@ UIService ui
#@ LogService logService
#@ StatusService statusService

/**
 * AimSeg is a bioimage analysis tool for axon, inner tongue and myelin segmentation
 * It has been deployed as a Groovy script for ImageJ. The latest relese is distributed
 * through the AimSeg update site in Fiji:
 *
 * 		http://sites.imagej.net/AimSeg/http://sites.imagej.net/AimSeg/
 *
 * AimSeg is a semiautomated workflow that combines automated processing with user
 * edition to enable a better segmentation. The workflow relies on the combination
 * of ImageJ with machine learning toolkits to improve the segmentation of electron
 * microscopy images. Specifically, it has been designed to work with ilastik, but
 * similar toolkits have the potential to be combined with AimSeg (e.g., Weka).
 *
 * AimSeg requires as imput:
 * 		- An electron microscopy image (raw or pre-processed)
 * 		- A probability map
 * 		- An object prediction
 *
 * The probability map must contain a channel corresponding to the compact myelin
 * probability, whereas the object prediction should contain axon instances.
 * Documentation on how to perform the training of the pixel and object classifiers
 * in ilastik is available in GitHub:
 *
 * 		- https://github.com/paucabar/AimSeg/blob/master/README.md
 *
 * The AimSeg workflow is subdivided in three stages each corresponding to the
 * segmentation of three components of the fibre cross-section (RoiSet):
 * 		- The axon
 * 		- The inner compact myelin layer (ICML), i.e., axon + inner tongue
 * 		- The fibre, i.e, axon + inner tongue + compact myelin
 *
 * For each stage, AimSeg proposes a segmentation output that the user can
 * correct before proceeding to the next stage (Stage1:ICML, Stage2:fibre,
 * Stage3:axon). A post-processing pipeline amend some typical mistakes
 * introduced by hand drawing Rois and labels each Roi to establish a
 * hierarchy between the 3 RoiSets (Fibre > ICML > Axon). Finally, the
 * area of the Rois is quantified and stores in a results table
 * (rows:labels;colums:RoiSets)
 *
 * Pau Carrillo Barberà
 * Instituto de Biotecnología y Biomedicina (BioTecMed), Universitat de València (Valencia, Spain)
 * Institute of Genetics and Cancer, University of Edinburgh (Edinburgh, United Kingdom)
 * Last version 01/09/2022
 */

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
import ij.plugin.Commands
import ij.process.FloatPolygon
import ij.gui.PolygonRoi
import ij.gui.ShapeRoi
import ij.measure.Calibration

/**
 * Checks if an update site is active
 * If the update site is not active, shows a dialog asking the user to activate it
 */
boolean isUpdateSiteActive (String updateSite) {
    boolean checkUpdate = true
    if (! updateService.getUpdateSite(updateSite).isActive()) {
        ui.showDialog "Please activate the $updateSite update site"
        checkUpdate = false
    }
    return checkUpdate
}

/**
 * Installs the AimSeg macro that contains the shortcuts for the user edition
 */
def installMacro (boolean toolsetMacro) {
    String macroPath
    if(toolsetMacro) {
        String toolsetsPath = IJ.getDir("macros") + "toolsets"
        macroPath = IJ.addSeparator(toolsetsPath)+"AimSeg_Macros.ijm"
    } else {
        macroPath = IJ.getDir("macros") + "StartupMacros.fiji.ijm"
    }
    IJ.run("Install...", "install=[${->macroPath}]")
}

/**
 * Close any open image or RoiManager instance
 */
def cleanUp() {
    RoiManager rm = RoiManager.getRawInstance()
    if(rm != null) {
        if(rm.getCount() > 0) {
        	rm.deselect()
        	rm.runCommand("Delete")
        }
        rm.close()
    }
    Commands cmd = new Commands()
    cmd.closeAll()
}

/**
 * Opens an image file
 * If the file extension is h5, the image is imported using the ilastik's importer
 * Otherwise, the file is imported using ImageJ's opener
 */
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

/**
 * Implements the Erode, Dilate, Open and Close operations using the doIterations method (see below)
 * This method is modified from the source code of the commands in the ImageJ's Process/Binary submenu
 */
def run (ImageProcessor ip, String operation, int iterations, int count) {
    int fg = Prefs.blackBackground ? 255 : 0
    int foreground = ip.isInvertedLut() ? 255-fg : fg
    int background = 255 - foreground
    ip.setSnapshotCopyMode(true)

    if (operation.equals("erode") || operation.equals("dilate")) {
        doIterations((ByteProcessor)ip, operation, iterations, count, background)
    } else if (operation.equals("open")) {
        doIterations(ip, "erode", iterations, count, background)
        doIterations(ip, "dilate", iterations, count, background)
    } else if (operation.equals("close")) {
        doIterations(ip, "dilate", iterations, count, background)
        doIterations(ip, "erode", iterations, count, background)
    }
    ip.setSnapshotCopyMode(false)
    ip.setBinaryThreshold()
}

/**
 * Implements the Erode and Dilate operations
 * This method is modified from the source code of the commands in the ImageJ's Process/Binary submenu
 */
def doIterations (ImageProcessor ip, String operation, int iterations, int count, int background) {
    for (int i=0; i<iterations; i++) {
        if (Thread.currentThread().isInterrupted()) return
        if (IJ.escapePressed()) {
            boolean escapePressed = true
            ip.reset()
            return
        }
        if (operation.equals("erode"))
            ((ByteProcessor)ip).erode(count, background)
        else
            ((ByteProcessor)ip).dilate(count, background)
    }
}

/**
 * Implements the Binary fill operation
 * This method is modified from the source code of the commands in the ImageJ's Process/Binary submenu
 * Contributed by Gabriel Landini, G.Landini at bham.ac.uk
 * 21/May/2008
 */
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

/**
 * Implements ImageJ's Particle Analyzer
 * The method will always return an ImagePlus
 * options is defined as an integer using ParticleAnalyzer fields
 * options is defined as an integer using Interface Measurements fields
 * results table is not given as an argument because the method is never used to measure
 */
ImagePlus analyzeParticles (ImagePlus imp, int options, int measurements, double minSize, double maxSize, double minCirc, double maxCirc) {
    def rt = new ResultsTable()
    def pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize, minCirc, maxCirc)
    ImageProcessor ip = imp.getProcessor()
    ip.setBinaryThreshold()
    pa.setHideOutputImage(true)
    pa.analyze(imp, ip)
    ImagePlus impOutput = pa.getOutputImage()
    if (impOutput.isInvertedLut()) {
        IJ.run(impOutput, "Grays", "") // get the non-inverted LUT
    }
    return impOutput
}

/**
 * If there are group 1 ROIs, these are deleted
 * If there are group 2 ROIs, these are set as group 0
 */
def cleanRoiSet (ImagePlus imp, RoiManager rm) {
    rm.selectGroup(1)
    if(rm.selected() > 0) {
        rm.runCommand(imp, "Delete")
    }
    rm.selectGroup(2)
    if(rm.selected() > 0) {
        rm.setGroup(0)
        rm.runCommand(imp, "Deselect")
    }
    rm.getRoisAsArray().eachWithIndex { roi, index ->
        roi.setColor(Color.YELLOW)
    }
}

/**
 * Creates a binary from the ROI Manager
 */
ImagePlus createRoiMask (ImagePlus imp, RoiManager rm) {
    IJ.run(imp, "Select None", "");
    rm.deselect()
    rm.runCommand(imp,"Combine")
    ByteProcessor mask = imp.createRoiMask()
    ImagePlus impMask = new ImagePlus("Mask", mask)
    return impMask
}

/**
 * Creates an RGB image adding a binary mask as an overlay to the EM image
 * The overlay is filled with a specific color and alpha (transparency, 8-bit)
 * The output is a flatten image to save memory
 */
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

/**
 * Implements Morphology's Binary Reconstruct, by Landini
 * It requires two ImagePlus, a mask (imp1) and a seed (imp2)
 * The method is set to not show a new image
 * Insted, reconstruction is performed on seed image
 * To create a new image change name (null) by a string and set create image true
 */
def runBinaryReconstruct (ImagePlus imp1, ImagePlus imp2) {
    def BinaryReconstruct_ br = new BinaryReconstruct_()
    Object[] result = br.exec(imp1, imp2, null, false, true, false )
    //parameters above are: mask ImagePlus, seed ImagePlus, name, create new image, white particles, connect4
    if (null != result) {
        String name = (String) result[0]
        ImagePlus recons = (ImagePlus) result[1]
    }
}

/**
 * Transfers properties from ROI 1 to ROI 2
 */
def transferProperties (Roi roi1, Roi roi2) {
    if (roi1==null || roi2==null)
        return
    roi2.setStrokeColor(roi1.getStrokeColor())
    if (roi1.getStroke()!=null)
        roi2.setStroke(roi1.getStroke())
    roi2.setDrawOffset(roi1.getDrawOffset())
    if (roi1.getGroup()!=null)
        roi2.setGroup(roi1.getGroup())
    if (roi1.getStrokeWidth()!=null)
        roi2.setStrokeWidth(roi1.getStrokeWidth())
}

/**
 * Replaces the original ROIs by their convex hull ROIs
 * Always select ROI index 0, as it is deleted at the end of the loop
 * New ROIs are added at the the end of the list
 */
def convexHull (RoiManager rm) {
    int roiCount = rm.getCount()
    println "$roiCount selected ROIs"
    rm.getRoisAsArray().eachWithIndex { roi, index ->
        if (roi != null) {
            rm.select(index)
            FloatPolygon p = roi.getFloatConvexHull()
            Roi roi2 = new PolygonRoi(p, Roi.POLYGON)
            transferProperties(roi, roi2)
            rm.setRoi(roi2, index)
        }
    }
}

/**
 * Replaces composite ROIs (ShapeRois) by the largest ROI within the ShapeRoi instance
 */
def replaceShapeRois(RoiManager rm) {
    rm.getRoisAsArray().eachWithIndex { roi, index ->
        if (roi instanceof ShapeRoi) {
            // Split the shape ROI
            def rois = ((ShapeRoi)roi).getRois()
            if (rois.size() > 1) {
                // Get the areas of all ROIs and find the largest
                def areas = rois.collect(r -> r.getStatistics().area)
                int indLargest = areas.indexOf(areas.max())
                assert indLargest >= 0 // Must happen or something has gone wrong (float comparison)
                def roiLargest = rois[indLargest]
                // replace the ShapeRoi by the largest Roi in the composite selection, removing the smaller ones
                rm.setRoi(rois[indLargest], index)
            }
        }
    }
}

/**
 * Creates a label image from the RoiManager
 * Uses Roi indexes as labels
 */
ImagePlus labelFromRois(ImagePlus imp, RoiManager rm) {
    impLabel = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
    ip = impLabel.getProcessor()
    rm.getRoisAsArray().eachWithIndex { roi, index ->
        ip.setColor(index+1)
        ip.fill(roi)
    }
    ip.resetMinAndMax()
    IJ.run(impLabel, "glasbey inverted", "")
    return impLabel
}

/**
 * Gets the intersection of each Roi pair (according to key) from two maps
 * Updates the RoiManager corresponding to map2
 * The method ensures that Rois in map2 are always contained in their parent
 * (i.e., same key in map1)
 */
def roiIntersection(Map<String, Roi> map1, Map<String, Roi> map2, RoiManager rm2) {
	rm2.getRoisAsArray().eachWithIndex { roi, index ->
		if(map1[roi.getName()] != null && map2[roi.getName()]) {
			s1 = new ShapeRoi(map1[roi.getName()])
			s2 = new ShapeRoi(map2[roi.getName()])
			s3 = s1.and(s2)
			s3.setName(roi.getName())
			transferProperties(roi, s3)
			rm2.setRoi(s3, index)
			map2[roi.getName()] = s3
		}
	}
	return map2
}


/**
 * START
 */

// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik")
boolean checkMorphology = isUpdateSiteActive("Morphology")

// exit if any update site is missing
if (!checkIlastik || !checkMorphology) {
    return
}

// setup
installMacro(true)
cleanUp()

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

/**
 * import the corresponding probability map and object prediction
 * IMPORTANT:
 * 		- files must be stored in the imp parent folder
 * 		- files must be named after the imp filename, adding a tag as a suffix
 * 		- default tags work for default ilastik output
 * 		- tag for probability map = _Probabilities
 * 		- tag for object predictions = _Object Predictions
 */
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



/**
 * STAGE 1
 */

// timing
int t0s1 = System.currentTimeMillis()

// create myelin mask
impProb.setPosition(probChannel)
ImageProcessor ipProb = impProb.getProcessor()
ipProb.setThreshold (0.2, 1.0, ImageProcessor.NO_LUT_UPDATE)
ImageProcessor ipMyelinMask = ipProb.createMask() // image processor
ImagePlus impMyelinMask = new ImagePlus("Myelin Mask", ipMyelinMask) // image processor to image plus
impProb.close()

// duplicate and invert myelin mask
ImageProcessor ipMyelinMaskInverted = ipMyelinMask.duplicate()
ipMyelinMaskInverted.invert()
ImagePlus impMyelinMaskInverted = new ImagePlus("Myelin Inverted Mask", ipMyelinMaskInverted) // image processor to image plus

// exclude edges
int options_exclude_edges = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
int measurements_area = Measurements.AREA
ImagePlus impNoEdges = analyzeParticles(impMyelinMaskInverted, options_exclude_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)

// close and fill holes
Prefs.padEdges = false
run(impNoEdges.getProcessor(), "close", 2, 1)
Prefs.padEdges = true
fill(impNoEdges.getProcessor())

// image calculator XOR
def ic = new ImageCalculator()
ImagePlus impXOR = ic.run(impMyelinMaskInverted, impNoEdges, "XOR create")

// get axons on edges
int options_with_edges = ParticleAnalyzer.SHOW_MASKS
ImagePlus impEdges = analyzeParticles(impXOR, options_with_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0.3, 1)
impXOR.close()

// image calculator OR
ImagePlus impOR = ic.run(impNoEdges, impEdges, "OR create")
impNoEdges.close()
impEdges.close()

// get inner masks
int inMinArea = 10000 // min size to filter IN particles
int options_add_manager = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.ADD_TO_MANAGER
ImagePlus innerMasks = analyzeParticles(impOR, options_add_manager, measurements_area, inMinArea, Double.POSITIVE_INFINITY, 0.4, 1)
impOR.close()

// RoiManager set selected objects as group 2 (red ROIs)
RoiManager rm = RoiManager.getInstance()
rm.setVisible(false)
int roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
    roi.setColor(Color.RED)
}

// get other masks
ImagePlus allMasks = ic.run(innerMasks, impMyelinMaskInverted, "OR create")
impMyelinMaskInverted.close()
ImagePlus otherMasks = ic.run(innerMasks, allMasks, "XOR create")
innerMasks.close()
allMasks.close()
otherMasks = analyzeParticles(otherMasks, options_add_manager, measurements_area, 3000, 500000, 0, 1)
otherMasks.close()

// RoiManager set other objects as group 1 (blue ROIs)
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (index > roiCount -1) {
        roi.setGroup(1)
        roi.setStrokeWidth(5)
        roi.setColor(Color.BLUE)
    }
}

// wait for user
def wfu = new WaitForUserDialog("Edit ROIs", "If necessary, use the \"ROI Manager\" to edit\nthe output. Click \"OK\" once you finish")
if (!automated) wfu.show()


// discard group 1 ROIs and set group 2 ROIs as 0
cleanRoiSet(imp, rm)

// save ROI set IN
rm.deselect()
String parentPathS = imageFile.getParentFile()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
println "Save RoiSet_IN"

// timing
int t1s1 = System.currentTimeMillis()
int tS1 = t1s1-t0s1
println tS1

/**
 * PRE-STAGE 2
 */

// create IN final mask
ImagePlus maskIn = createRoiMask(imp, rm)
rm.runCommand(imp,"Delete")
imp.hide()
ImagePlus impMaskOverlayIN = setMaskOverlay(imp, maskIn, 127)
impMaskOverlayIN.show()

/**
 * STAGE 2
 */

// timing
int t0s2 = System.currentTimeMillis()

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
ImagePlus impMyelinAND = ic.run(impMyelinMask, maskIn, "AND create")
impMyelinMask = ic.run(impMyelinAND, impMyelinMask, "XOR create")
ImagePlus impMyelinOutlines = ic.run(impMyelinMask, impVoronoi, "AND create")
impVoronoi.close()
impMyelinMask.close()
fill(impMyelinOutlines.getProcessor())
ImagePlus impInToCount = analyzeParticles(maskIn, options_exclude_edges, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
maskIn.close()
run (impMyelinOutlines.getProcessor(), "open", 20, 1)
IJ.run(impMyelinOutlines, "Watershed", "")
ImagePlus impMyelinOutlines2 = impInToCount.duplicate()
runBinaryReconstruct(impMyelinOutlines, impMyelinOutlines2)
run (impMyelinOutlines2.getProcessor(), "close", 1, 1)
impMyelinOutlines2 = analyzeParticles(impMyelinOutlines2, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
impMyelinOutlines.close()
impMyelinOutlines2.close()

// RoiManager set selected objects as group 2 (red ROIs)
roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
    roi.setColor(Color.RED)
}

// wait for user
if (!automated) wfu.show()

// discard group 1 ROIs and set group 2 ROIs as 0
cleanRoiSet(impMaskOverlayIN, rm)

// save ROI set OUT
rm.deselect()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")
println "Save RoiSet_OUT"

// timing
int t1s2 = System.currentTimeMillis()
int tS2 = t1s2-t0s2
println tS2

/**
 * PRE-STAGE 3
 */

// create OUT final mask
ImagePlus impMaskOut = createRoiMask(impMaskOverlayIN, rm)
rm.runCommand(impMaskOverlayIN,"Delete")
impMaskOverlayIN.close()

// get myelin to count
ImagePlus impInToCountCor = impMaskOut.duplicate()
runBinaryReconstruct(impInToCount, impInToCountCor)
ImagePlus impMyelinToCount = ic.run(impInToCountCor, impMaskOut, "XOR create")
impInToCountCor.close()
ImagePlus impMaskOverlayMyelin = setMaskOverlay(imp, impMyelinToCount, 127)
impMyelinToCount.close()
impMaskOverlayMyelin.show()

/**
 * STAGE 3
 */

// timing
int t0s3 = System.currentTimeMillis()

// segment object predictions: select class
ImagePlus impAxonMasks = dup.run(impObj, 1, 1, 1, 1, 1, 1);
impAxonMasks.getProcessor().setThreshold(1, 2, ImageProcessor.NO_LUT_UPDATE)
impAxonMasks.setProcessor(impAxonMasks.getProcessor().createMask())
run (impAxonMasks.getProcessor(), "close", 5, 1)
fill(impAxonMasks.getProcessor())
impAxonMasks = ic.run(impAxonMasks, impMaskOut, "AND create")
impAxonMasks = analyzeParticles(impAxonMasks, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
impAxonMasks.close()

// get convex hull from ROIs
convexHull(rm)

// create convex hull mask
ImagePlus impConvexHullMask = createRoiMask(impMaskOverlayMyelin, rm)
rm.runCommand(impMaskOverlayMyelin,"Delete")

// correct convex hull and get ROIs
impConvexHullMask = ic.run(impConvexHullMask, impInToCount, "AND create")
impInToCount.close()
impConvexHullMask = analyzeParticles(impConvexHullMask, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
impConvexHullMask.close()

// RoiManager set selected objects as group 2 (red ROIs)
// get ROIs convex hull
roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
    roi.setColor(Color.RED)
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
impRejectMasks = ic.run(impRejectMasks, impMaskOut, "AND create")
impMaskOut.close()
fill(impRejectMasks.getProcessor())
impRejectMasks = analyzeParticles(impRejectMasks, options_add_manager, measurements_area, 0, Double.POSITIVE_INFINITY, 0, 1)
impObj.close()
impRejectMasks.close()

// RoiManager set other objects as group 1 (blue ROIs)
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (index > roiCount -1) {
        roi.setGroup(1)
        roi.setStrokeWidth(5)
        roi.setColor(Color.BLUE)
    }
}

// wait for user
if (!automated) wfu.show()

// discard group 1 ROIs and set group 2 ROIs as 0
cleanRoiSet(impMaskOverlayMyelin, rm)

// save ROI set AXON
rm.deselect()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")
println "Save RoiSet_AXON"
rm.deselect()
rm.runCommand(impMaskOverlayMyelin,"Delete")
impMaskOverlayMyelin.close()

// timing
int t1s3 = System.currentTimeMillis()
int tS3 = t1s3-t0s3
println tS3

/**
 * POST-PROCESSING
 */

// timing
int t0pp = System.currentTimeMillis()

// get calibration
cal = imp.getCalibration()
float calX = cal.getX(1)
float calY = cal.getY(1)
String calUnit = cal.getUnit()

// create 3 RoiManager
RoiManager rmIn = new RoiManager(false)
RoiManager rmOut = new RoiManager(false)
RoiManager rmAxon = new RoiManager(false)

// create 3 empty maps
def mapIn = [:]
def mapOut = [:]
def mapAxon = [:]

// replace ShapeRois from RoiSet_IN
//imp.show()
rmIn.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
//rm = rm.getInstance()
replaceShapeRois(rmIn)

// create IN label image
ImagePlus impLabelIN = labelFromRois (imp, rmIn)

// rename RoiSet_IN with 3-digit code and save
rmIn.getRoisAsArray().eachWithIndex { roi, index ->
    String nameTemp = String.format("%03d", index+1)
    rmIn.rename(index, nameTemp)
    mapIn[nameTemp] = roi
    mapOut[nameTemp] = null
    mapAxon[nameTemp] = null
}
rmIn.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")

// get IN count
int inCount = rmIn.getCount()

// clear RoiManager
//rm.deselect()
//rm.runCommand(imp,"Delete")

// replace ShapeRois from RoiSet_OUT
rmOut.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")
replaceShapeRois(rmOut)

// rename RoiSet_OUT with 3-digit code and save
rmOut.getRoisAsArray().eachWithIndex { roi, index ->
    impLabelIN.setRoi(roi)
    int code = roi.getStatistics().max as int
    String nameTemp2 = String.format("%03d", code)
    rmOut.rename(index, nameTemp2)
    mapOut[nameTemp2] = roi
}
rmOut.runCommand("Sort")
rmOut.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")

// measure OUT area
double[] areaListOut = [0] * inCount
rmOut.getRoisAsArray().eachWithIndex { roi, index ->
    def codeInt = roi.getName() as int
    areaListOut[codeInt-1] = roi.getStatistics().area * calX * calY
}
//println areaListOut

// clear RoiManager
//rm.deselect()
//rm.runCommand(imp,"Delete")

// make sure IN Rois do not overflow OUT Rois
//rm.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
mapIn = roiIntersection(mapOut, mapIn, rmIn)
rmIn.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
impLabelIN = labelFromRois(imp, rmIn)

// measure IN area
def roiListIn = rmIn.getRoisAsArray()
def areaListIn = roiListIn.collect(r -> r.getStatistics().area * calX * calY)
//println areaListIn

// clear RoiManager
//rm.deselect()
//rm.runCommand(imp,"Delete")

// replace IN result by 0 when there is no OUT Roi
for (i in 0..areaListIn.size()-1) {
    if(areaListOut[i] == 0) {
        areaListIn[i] = 0
    }
}

// replace ShapeRois from RoiSet_AXON
rmAxon.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")
replaceShapeRois(rmAxon)

// rename RoiSet_AXON with 3-digit code
rmAxon.getRoisAsArray().eachWithIndex { roi, index ->
    impLabelIN.setRoi(roi)
    code = roi.getStatistics().max as int
    String nameTemp3 = String.format("%03d", code)
    rmAxon.rename(index, nameTemp3)
    mapAxon[nameTemp3] = roi
}

// make sure AXON Rois do not overflow IN Rois
mapAxon = roiIntersection(mapIn, mapAxon, rmAxon)

// measure AXON area
double[] areaListAxon = [0] * areaListIn.size()
rmAxon.getRoisAsArray().eachWithIndex { roi, index ->
    codeInt = roi.getName() as int
    areaListAxon[codeInt-1] = roi.getStatistics().area * calX * calY
}
//println areaListAxon

// create and measure AXON Rois missing
if(addAxons) {
	for (i in 0..areaListIn.size()-1) {
	    if(areaListOut[i] != 0 && areaListAxon[i] == 0) {
	        ImageProcessor ipLabelIN = impLabelIN.getProcessor()
	        ipLabelIN.setThreshold (i+1, i+1)
	        def tts = new ThresholdToSelection()
	        Roi roiTemp = tts.convert(ipLabelIN)
	        roiTemp.setName(String.format("%03d", i+1))
	        roiTemp.setGroup(0)
	        roiTemp.setColor(Color.YELLOW)
	        roiTemp.setStrokeWidth(5)
	        //roiCount = rm.getCount()
	        rmAxon.addRoi(roiTemp)
	        areaListAxon[i] = roiTemp.getStatistics().area * calX * calY
	    }
	}
}
rmAxon.runCommand("Sort")
//println areaListAxon
//impLabelIN.show()

// save RoiSet_AXON
rmAxon.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")

/**
 * RESULTS TABLE
 */

// create and fill results table
ResultsTable rt = new ResultsTable(areaListIn.size())
rt.setPrecision(5)
rt.setValues("Axon", areaListAxon as double[])
rt.setValues("ICML", areaListIn as double[])
rt.setValues("Fibre", areaListOut as double[])

// set labels
for (i in 0..areaListIn.size()-1) {
    rt.setLabel(String.format("%03d", i+1), i)
}

// show results table
//rt.show("Results Table")

// save results table
rt.saveAs(parentPathS+File.separator+impNameWithoutExtension+"_Results.tsv")

// timing
int t1pp = System.currentTimeMillis()
int tPP = t1pp-t0pp
println tPP

/**
 * RESET
 */

// close any open image or RoiManager, reset Prefs and StartupMacros
rm.close()
rmIn.close()
rmOut.close()
rmAxon.close()
cleanUp()
installMacro(false)
Prefs.padEdges = pe
Prefs.blackBackground = bb

/**
 * LOG
 */
 
IJ.log("Timing Stage 1 (ms): $tS1")
IJ.log("Timing Stage 2 (ms): $tS2")
IJ.log("Timing Stage 3 (ms): $tS3")
IJ.log("Timing Post-processing (ms): $tPP")
if(calUnit == "pixel") {
	IJ.log("Area unit: pixel")
} else {
	IJ.log("Area unit: squared $calUnit")
}
IJ.log("AimSeg processing finished")
return