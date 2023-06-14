import ij.plugin.frame.RoiManager

RoiManager rm = new RoiManager()
rm = rm.getInstance()
boolean showing = rm.isVisible()
if (showing) {
	rm.setVisible(false)
} else {
	rm.setVisible(true)
}