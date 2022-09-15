#@ File(label="Image File", style="open") imageFile
#@ boolean (label="Axon Autocomplete", value=true, persist=false) autocomplete
#@ UpdateService updateService
#@ UIService ui
#@ LogService logService
#@ StatusService statusService

import ij.IJ
import ij.Prefs
import ij.io.Opener
import ij.ImagePlus
import groovy.io.FileType
import net.imglib2.img.display.imagej.ImageJFunctions
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader
import ij.plugin.frame.RoiManager
import ij.gui.Roi
import java.awt.Color
import ij.plugin.Commands
import ij.gui.ShapeRoi

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