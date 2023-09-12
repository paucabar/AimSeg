import ij.plugin.frame.RoiManager

RoiManager rm = RoiManager.getInstance()

// rename roi set with 3-digit code based on index
rm.getRoisAsArray().eachWithIndex { roi, index ->
    String nameTemp = String.format("%03d", index+1)
    rm.rename(index, nameTemp)
}
return