#@ File(label="Image File", style="open") file

import ij.IJ
import ij.io.Opener
import ij.ImagePlus

String imagePath = file.getAbsolutePath()
println imagePath

//def fi = new FileInfo()
def opener = new Opener()
imp = opener.openImage(imagePath)
imp.show()