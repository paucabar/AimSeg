#@ ImagePlus imp

import ij.gui.ImageCanvas

ImageCanvas ic = imp.getCanvas()
boolean showingAll = ic.getShowAllROIs()
if (showingAll) {
	ic.setShowAllROIs(false)
} else {
	ic.setShowAllROIs(true)
}
