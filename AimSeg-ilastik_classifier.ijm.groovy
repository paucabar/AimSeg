#@ File(label="Image File", style="open") file

import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import ij.gui.WaitForUserDialog

// setup
importImage(file)
installMacro()

def wfu = new WaitForUserDialog("Title", "I'm waiting")
wfu.show()

def installMacro (){
	String toolsetsPath = IJ.getDir("macros") + "toolsets"
	String ijmPath = IJ.addSeparator(toolsetsPath)+"Toggle_ROI_color.ijm"
	IJ.run("Install...", "install=[${->ijmPath}]")
}

def importImage (inputFile){
	String imagePath = inputFile.getAbsolutePath()
	println imagePath
	def opener = new Opener()
	imp = opener.openImage(imagePath)
	imp.show()
}