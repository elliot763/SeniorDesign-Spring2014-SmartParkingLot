

/**
 * 
 * @author Elliot Dean
 */
public class EntranceController {
	
	EntranceDisplay display;

	public static void main(String[] args) throws InterruptedException {
		
		final EntranceController controller = new EntranceController();
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                controller.display = EntranceDisplay.createAndShowGUI();
            } // run
        });
		
		Thread.sleep(500);
		controller.display.addSpace(278, 305);		
		controller.display.addSpace(121, 67);
		controller.display.updateUI();
		
	} // main
	
	private EntranceController() {
		
	} // EntranceController
	
} // EntranceController - Class
