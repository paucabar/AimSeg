#@ File(label="Image File", style="open") imageFile
#@ Integer (label="Myelin Probability Channel", value=1, max=3, min=1, style="listBox") probChannel
#@ String (label="Object Prediction Threshold", choices={"Below", "Above"}, value="Above", style="radioButtonHorizontal") objThr
#@ Integer (label="Object Prediction Label", value=2, max=10, min=1, style="listBox") objLabel
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
import ij.plugin.RGBStackMerge
import ij.plugin.RGBStackConverter
import ij.CompositeImage
import ij.process.LUT
import java.awt.Color
import ij.plugin.Duplicator
import inra.ijpb.watershed.MarkerControlledWatershedTransform2D


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

// Implements the Erode, Dilate, Open and Close commands in the Process/Binary submenu
public void run (ImageProcessor ip, String arg, int iter, int cnt) {
    int fg = Prefs.blackBackground ? 255 : 0;
    foreground = ip.isInvertedLut() ? 255-fg : fg;
    background = 255 - foreground;
    ip.setSnapshotCopyMode(true);

	if (arg.equals("erode") || arg.equals("dilate")) {
        doIterations((ByteProcessor)ip, arg, iter, cnt);
	} else if (arg.equals("open")) {
        doIterations(ip, "erode", iter, cnt);
        doIterations(ip, "dilate", iter, cnt);
    } else if (arg.equals("close")) {
        doIterations(ip, "dilate", iter, cnt);
        doIterations(ip, "erode", iter, cnt);
    }
    ip.setSnapshotCopyMode(false);
    ip.setBinaryThreshold();
}

void doIterations (ImageProcessor ip, String mode, int iterations, int count) {
    for (int i=0; i<iterations; i++) {
        if (Thread.currentThread().isInterrupted()) return;
        if (IJ.escapePressed()) {
            escapePressed = true;
            ip.reset();
            return;
        }
        if (mode.equals("erode"))
            ((ByteProcessor)ip).erode(count, background);
        else
            ((ByteProcessor)ip).dilate(count, background);
    }
}

// Binary fill by Gabriel Landini, G.Landini at bham.ac.uk
// 21/May/2008
void fill(ImageProcessor ip) {
    int fg = Prefs.blackBackground ? 255 : 0;
    foreground = ip.isInvertedLut() ? 255-fg : fg;
    background = 255 - foreground;
    ip.setSnapshotCopyMode(true);
    
    int width = ip.getWidth();
    int height = ip.getHeight();
    FloodFiller ff = new FloodFiller(ip);
    ip.setColor(127);
    for (int y=0; y<height; y++) {
        if (ip.getPixel(0,y)==background) ff.fill(0, y);
        if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y);
    }
    for (int x=0; x<width; x++){
        if (ip.getPixel(x,0)==background) ff.fill(x, 0);
        if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1);
    }
    byte[] pixels = (byte[])ip.getPixels();
    int n = width*height;
    for (int i=0; i<n; i++) {
    if (pixels[i]==127)
        pixels[i] = (byte)background;
    else
        pixels[i] = (byte)foreground;
    }
}

def analyzeParticles(ImagePlus imp, int options, int measurements, double minSize, double maxSize, double minCirc, double maxCirc) {
	def rt = new ResultsTable();
	def pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize, minCirc, maxCirc)
	ip = imp.getProcessor()
	ip.setBinaryThreshold()
	pa.setHideOutputImage(true)
	pa.analyze(imp, ip)
	def impOutput = pa.getOutputImage()
	if (impOutput.isInvertedLut()) {
		IJ.run(impOutput, "Grays", "")
	}
	return impOutput
}

def runMarkerControlledWatershed(ImageProcessor input, ImageProcessor labels, ImageProcessor mask, int connectivity) {
	mcwt = new MarkerControlledWatershedTransform2D (input, labels, mask, connectivity)
	result = mcwt.applyWithPriorityQueue()
	ImagePlus impResult = new ImagePlus("Out_to_count", result)
	return impResult
}





// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik");

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

// create myelin mask
impProb.setPosition(probChannel)
def ipProb = impProb.getProcessor()
ipProb.setThreshold (0.2, 1.0)
def ipMyelinMask = ipProb.createMask() // image processor
def impMyelinMask = new ImagePlus("Myelin Mask", ipMyelinMask) // image plus
//impMyelinMask.show()

// duplicate and invert myelin mask
def ipMyelinMaskInverted = ipMyelinMask.duplicate()
ipMyelinMaskInverted.invert()
def impMyelinMaskInverted = new ImagePlus("Myelin Inverted Mask", ipMyelinMaskInverted)
//impMyelinMaskInverted.show()

// exclude edges
int options_exclude_edges = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
int measurements_area = Measurements.AREA
impNoEdges = analyzeParticles(impMyelinMaskInverted, options_exclude_edges, measurements_area, 0, 999999, 0, 1)
//impNoEdges.show()

// close and fill holes
ipNoEdges = impNoEdges.getProcessor()
run(ipNoEdges, "close", 2, 1)
fill(ipNoEdges)

// image calculator XOR
def ic = new ImageCalculator()
def impXOR = ic.run(impMyelinMaskInverted, impNoEdges, "XOR create")
//impXOR.show()

// get axons on edges
int options_with_edges = ParticleAnalyzer.SHOW_MASKS
impEdges = analyzeParticles(impXOR, options_with_edges, measurements_area, 0, 999999, 0.3, 1)
//impEdges.show()

// image calculator OR
def impOR = ic.run(impNoEdges, impEdges, "OR create")
//impOR.show()

// get inner masks
int options_add_manager = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.ADD_TO_MANAGER
innerMasks = analyzeParticles(impOR, options_add_manager, measurements_area, 3000, 999999, 0.4, 1)
//innerMasks.show()

// RoiManager set selected objects as group 2 (red ROIs)
rm = RoiManager.getInstance()
int roiCount = rm.getCount()
println "$roiCount selected ROIs"
rm.getRoisAsArray().eachWithIndex { roi, index ->
    roi.setGroup(2)
    roi.setStrokeWidth(5)
}

// get other masks
def allMasks = ic.run(innerMasks, impMyelinMaskInverted, "OR create")
def otherMasks = ic.run(innerMasks, allMasks, "XOR create")
otherMasksSizeFilter = analyzeParticles(otherMasks, options_add_manager, measurements_area, 3000, 500000, 0, 1)

// RoiManager set other objects as group 1 (blue ROIs)
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (index > roiCount -1) {
	    roi.setGroup(1)
	    roi.setStrokeWidth(5)
    }
}

// wait for user
def wfu = new WaitForUserDialog("Edit ROIs", "If necessary, use the \"ROI Manager\" to edit\nthe output. Click \"OK\" once you finish")
wfu.show()

// discard group 1 ROIs
def roiDiscard = []
int count = rm.getCount()
rm.getRoisAsArray().eachWithIndex { roi, index ->
    if (roi.getGroup() == 1) {
	    roiDiscard.add(index)
    } else {
    	roi.setGroup(0)
    	roi.setStrokeWidth(0)
    }
}
println "Discard ROIs $roiDiscard"
def roiDiscardInt = roiDiscard as int[]
rm.setSelectedIndexes(roiDiscardInt)
rm.runCommand(imp,"Delete")

// save ROI set IN
rm.deselect()
String parentPathS = imageFile.getParentFile()
rm.save(parentPathS+File.separator+impNameWithoutExtension+"_RoiSet_IN.zip")
println "Save RoiSet_IN"

//////////////
// PRE-STAGE 2
//////////////

// create IN final mask
IJ.run(imp, "Select None", "");
rm.deselect()
rm.runCommand(imp,"Combine")
ByteProcessor mask = imp.createRoiMask()
ImagePlus maskIn = new ImagePlus("IN Mask", mask);
rm.runCommand(imp,"Delete")
//rm.close()
imp.hide()

// merge channels to display
ImagePlus[] impMergeIN = [imp, maskIn]
def rgbSMerge = new RGBStackMerge()
ImagePlus compositeIN = rgbSMerge.mergeChannels(impMergeIN, true)
//println compositeIN.getClass()

// set composite LUTs
luts = compositeIN.getLuts()
luts[0] = LUT.createLutFromColor(Color.GRAY)
luts[1] = LUT.createLutFromColor(Color.MAGENTA)
compositeIN.setLuts(luts)
compositeIN.updateAndDraw()

// convert to RGB
def rgbSConverter = new RGBStackConverter()
rgbSConverter.convertToRGB(compositeIN)
compositeIN.show()

//////////////
// STAGE 2
//////////////

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
ImagePlus impInToCount = analyzeParticles(maskIn, options_exclude_edges, measurements_area, 0, 999999, 0, 1)
run (myelinOutlines.getProcessor(), "open", 20, 1)
IJ.run(myelinOutlines, "Watershed", "")
int options_count_masks = ParticleAnalyzer.SHOW_ROI_MASKS
ImagePlus impInToCountLabels = analyzeParticles(impInToCount, options_count_masks, measurements_area, 0, 999999, 0, 1)
ImagePlus myelinOutlines2 = runMarkerControlledWatershed(myelinOutlines.getProcessor(), impInToCountLabels.getProcessor(), myelinOutlines.getProcessor(), 8)
myelinOutlines2.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE)
myelinOutlines2.setProcessor(myelinOutlines2.getProcessor().createMask())
run (myelinOutlines2.getProcessor(), "close", 1, 1)
ImagePlus impOutMasks = analyzeParticles(myelinOutlines2, options_add_manager, measurements_area, 0, 999999, 0, 1)


// reset Prefs.padEdges
Prefs.padEdges = pe
Prefs.blackBackground = bb