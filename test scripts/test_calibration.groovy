#@ ImagePlus imp

import ij.ImagePlus
import ij.measure.Calibration

cal = imp.getCalibration()
println cal.getX(1)
println cal.getY(1)
println cal.getUnit()