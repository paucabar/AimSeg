#@ File(label="Image File", style="open") imageFile
#@ boolean (label="Axon Autocomplete", value=true, persist=false) autocomplete
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

ImagePlus labelFromRoiCodes(ImagePlus imp, RoiManager rm) {
	impLabel = IJ.createImage("Labeling", "16-bit black", imp.getWidth(), imp.getHeight(), 1)
	ip = impLabel.getProcessor()
	rm.getRoisAsArray().eachWithIndex { roi, index ->
		def codeInt = roi.getName() as int
		ip.setColor(codeInt)
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

def excludeEdgesRois(ImagePlus imp, RoiManager rm) {
	rm.runCommand(imp, "Deselect")
	rm.setGroup(0)
	rm.getRoisAsArray().eachWithIndex { roi, index ->
		imp.setRoi(roi)
		//println roi.getStatistics().roiX
		//println roi.getStatistics().roiY
		if (roi.getStatistics().roiX == 0 || roi.getStatistics().roiY == 0) {
			rm.select(index)
			rm.setGroup(1)
		}
		if (roi.getStatistics().roiX + roi.getStatistics().roiWidth == imp.width || roi.getStatistics().roiY + roi.getStatistics().roiHeight == imp.height) {
			rm.select(index)
			rm.setGroup(1)
		}
	}
	rm.runCommand(imp, "Deselect")
	cleanRoiSet (imp, rm)
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
//imp.show()

File parentPath = imageFile.getParentFile()
def fileList = []
parentPath.eachFile(FileType.FILES) {
    fileList << it.name
}
String impNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))
String parentPathS = imageFile.getParentFile()

// get calibration
cal = imp.getCalibration()
float calX = cal.getX(1)
float calY = cal.getY(1)
String calUnit = cal.getUnit()

// create 3 RoiManager
RoiManager rmOut = new RoiManager(false)
RoiManager rmIn = new RoiManager(false)
RoiManager rmAxon = new RoiManager(false)

// create 3 empty maps
def mapIn = [:]
def mapOut = [:]
def mapAxon = [:]

// replace ShapeRois from RoiSet_OUT
rmOut.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")
replaceShapeRois(rmOut)

// exclude edge Rois
excludeEdgesRois(imp, rmOut)

// create OUT label image
ImagePlus impLabelOUT = labelFromRois (imp, rmOut)

// rename RoiSet_OUT with 3-digit code and save
rmOut.getRoisAsArray().eachWithIndex { roi, index ->
    String nameTemp = String.format("%03d", index+1)
    rmOut.rename(index, nameTemp)
    mapOut[nameTemp] = roi
    mapIn[nameTemp] = null
    mapAxon[nameTemp] = null
}
rmOut.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")

// replace ShapeRois from RoiSet_IN
rmIn.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
replaceShapeRois(rmIn)

// rename RoiSet_In with 3-digit code and save
excludeEdgesRois(imp, rmIn)
rmIn.getRoisAsArray().eachWithIndex { roi, index ->
    impLabelOUT.setRoi(roi)
    int code = roi.getStatistics().max as int
    if(code > 0) {
	    String nameTemp2 = String.format("%03d", code)
	    rmIn.rename(index, nameTemp2)
	    mapIn[nameTemp2] = roi
    } else {
		rmIn.select(index)
		rmIn.setGroup(1)
    }
}
cleanRoiSet (imp, rmIn)
rmIn.runCommand("Sort")
rmIn.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")

// clean Out Rois with no In Roi
ImagePlus impLabelIN = labelFromRois (imp, rmIn)
rmOut.getRoisAsArray().eachWithIndex { roi, index ->
    impLabelIN.setRoi(roi)
    int code = roi.getStatistics().max as int
    if(code == 0) {
		rmOut.select(index)
		rmOut.setGroup(1)
    }
}
cleanRoiSet (imp, rmOut)
rmOut.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_OUT.zip")

// make sure IN Rois do not overflow OUT Rois
//rm.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
mapIn = roiIntersection(mapOut, mapIn, rmIn)
rmIn.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")

impLabelOUT = labelFromRoiCodes(imp, rmOut)

// rename RoiSet_AXON with 3-digit code
rmAxon.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")
rmAxon.getRoisAsArray().eachWithIndex { roi, index ->
    impLabelOUT.setRoi(roi)
    code = roi.getStatistics().max as int
    if(code > 0) {
	    String nameTemp3 = String.format("%03d", code)
	    rmAxon.rename(index, nameTemp3)
	    mapAxon[nameTemp3] = roi
    } else {
		rmAxon.select(index)
		rmAxon.setGroup(1)
    }
}
cleanRoiSet (imp, rmAxon)

// make sure AXON Rois do not overflow IN Rois
mapAxon = roiIntersection(mapIn, mapAxon, rmAxon)

// create and measure AXON Rois missing
if(autocomplete) {
	for (i in 1..mapIn.size()) {
	    if(mapIn[String.format("%03d", i)] != null && mapAxon[String.format("%03d", i)] == null) {
	        Roi roiTemp = mapIn[String.format("%03d", i)]
	        roiTemp.setName(String.format("%03d", i))
	        //roiTemp.setGroup(0)
	        //roiTemp.setColor(Color.YELLOW)
	        //roiTemp.setStrokeWidth(5)
	        //roiCount = rm.getCount()
	        rmAxon.addRoi(roiTemp)
	        //areaListAxon[i] = roiTemp.getStatistics().area * calX * calY
	    }
	}
}
rmAxon.runCommand("Sort")

// save RoiSet_AXON
rmAxon.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")

return

// measure IN area
def roiListIn = rmIn.getRoisAsArray()
def areaListIn = roiListIn.collect(r -> r.getStatistics().area * calX * calY)

// measure OUT area
double[] areaListOut = [0] * inCount
rmOut.getRoisAsArray().eachWithIndex { roi, index ->
    def codeInt = roi.getName() as int
    areaListOut[codeInt-1] = roi.getStatistics().area * calX * calY
}
//println areaListOut


//println areaListIn


// replace ShapeRois from RoiSet_AXON
rmAxon.open(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_AXON.zip")
replaceShapeRois(rmAxon)



// measure AXON area
double[] areaListAxon = [0] * areaListIn.size()
rmAxon.getRoisAsArray().eachWithIndex { roi, index ->
    codeInt = roi.getName() as int
    areaListAxon[codeInt-1] = roi.getStatistics().area * calX * calY
}
//println areaListAxon



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

/**
 * RESET
 */

// close any open image or RoiManager, reset Prefs and StartupMacros
rmIn.close()
rmOut.close()
rmAxon.close()
cleanUp()
Prefs.padEdges = pe
Prefs.blackBackground = bb

IJ.log("AimSeg processing finished")
return