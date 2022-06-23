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
import ij.process.ByteProcessor
import ij.process.FloodFiller
import ij.plugin.filter.ParticleAnalyzer
import ij.measure.ResultsTable
import ij.measure.Measurements
import ij.plugin.ImageCalculator
import ij.gui.WaitForUserDialog
import net.imglib2.img.display.imagej.ImageJFunctions
import org.ilastik.ilastik4ij.hdf5.Hdf5DataSetReader


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





// check update sites
boolean checkIlastik = isUpdateSiteActive("ilastik");

// setup
installMacro()
Prefs.blackBackground=true
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
String probNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))+"_Probabilities"
String objNameWithoutExtension = imageFile.name.take(imageFile.name.lastIndexOf('.'))+"_Object Predictions"
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
impMyelinMaskInverted.show()

// exclude edges and big objects
Integer options = ParticleAnalyzer.SHOW_MASKS + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
Integer measurements = Measurements.AREA
def rt = new ResultsTable();
def pa = new ParticleAnalyzer(options, measurements, rt, 10000, 999999)
pa.setHideOutputImage(true)
pa.analyze(impMyelinMaskInverted)
def impNoEdges = pa.getOutputImage()
if (impNoEdges.isInvertedLut()) {
	IJ.run(impNoEdges, "Grays", "")
}
impNoEdges.show()

// close and fill holes
ipNoEdges = impNoEdges.getProcessor()
run(ipNoEdges, "close", 2, 1)
//IJ.run(impNoEdges, "Options...", "iterations=1 count=1 do=[Fill Holes]");
fill(ipNoEdges)
return

// image calculator
def ic = new ImageCalculator()
def impXOR = ic.run(impMyelinMaskInverted, impNoEdges, "XOR create")
impXOR.show()

// wait for user
def wfu = new WaitForUserDialog("Title", "I'm waiting")
wfu.show()

// reset Prefs.padEdges
Prefs.padEdges = pe