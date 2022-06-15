#@ ImagePlus imp
#@ ConvertService convertService
#@ OpService ops
#@ UIService ui
#@ Integer(label="Structuring element width", min=1, value=1) width

data = convertService.convert(imp, Dataset)

import net.imglib2.algorithm.neighborhood.*
import net.imagej.Dataset

shape = new DiamondShape(width)

// create new image
//import net.imglib2.type.numeric.integer.*
//result = ops.create().img(data, new UnsignedByteType())
//ops.morphology().close(result, data, [shape])
//ui.show(result)

// apply on imp
ops.morphology().open(data, data, [shape])
