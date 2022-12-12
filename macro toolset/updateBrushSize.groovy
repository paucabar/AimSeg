#@ int (label="Increase", value=3, persist=true) increase

import ij.gui.Toolbar

def round3(x) {
    return (int)Math.round(x/3)*3
}

def updateBrushSize(int inc_value) {
	Toolbar tb = new Toolbar()
	tb.setBrushSize(round3(tb.getBrushSize() + inc_value))
	println tb.getBrushSize()
}

updateBrushSize(increase)