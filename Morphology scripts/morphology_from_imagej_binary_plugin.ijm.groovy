#@ ImagePlus imp
#@ Integer (label="Iterations", value=1, max=99, min=1, style="listBox") iterations
#@ Integer (label="Count", value=1, max=8, min=1, style="listBox") count  
#@ String (label="Thresholding Method", choices={"erode", "dilate", "open", "close"}, value="erode", style="listBox") arg

import ij.*;
import ij.process.*;

// Implements the Erode, Dilate, Open and Close commands in the Process/Binary submenu. 

public void run (ImageProcessor ip) {
    int fg = Prefs.blackBackground ? 255 : 0;
    foreground = ip.isInvertedLut() ? 255-fg : fg;
    background = 255 - foreground;
    ip.setSnapshotCopyMode(true);
    
    //String arg = "erode"
	if (arg.equals("erode") || arg.equals("dilate")) {
        doIterations((ByteProcessor)ip, arg);
	} else if (arg.equals("open")) {
        doIterations(ip, "erode");
        doIterations(ip, "dilate");
    } else if (arg.equals("close")) {
        doIterations(ip, "dilate");
        doIterations(ip, "erode");
    }
    ip.setSnapshotCopyMode(false);
    ip.setBinaryThreshold();
}

void doIterations (ImageProcessor ip, String mode) {
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

Prefs.padEdges = true
impo = imp.getProcessor()
run(impo)