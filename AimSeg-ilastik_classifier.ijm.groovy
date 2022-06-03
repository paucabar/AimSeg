#@ File(label="Image File", style="open") file

import ij.IJ
import ij.io.Opener
import ij.ImagePlus
import ij.gui.WaitForUserDialog

importImage(file)

def importImage (inputFile){
	String imagePath = inputFile.getAbsolutePath()
	println imagePath
	def opener = new Opener()
	imp = opener.openImage(imagePath)
	imp.show()
}

def wfu = new WaitForUserDialog("Title", "I'm waiting")
wfu.show()