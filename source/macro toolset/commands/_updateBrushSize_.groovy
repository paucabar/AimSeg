#@ int (label="Increase", value=3, persist=false) increase

import ij.gui.Toolbar

def round3(x) {
    return (int)Math.round(x/3)*3
}

def updateBrushSize(int inc_value) {
	Toolbar tb = new Toolbar()
	def newSize = round3(tb.getBrushSize() + inc_value)
	if (newSize > 0) {	
		tb.setBrushSize(newSize)
		println tb.getBrushSize()
	} else {
		tb.setBrushSize(1)
	}
	println("Brush Size updated: $newSize")
}

updateBrushSize(increase)