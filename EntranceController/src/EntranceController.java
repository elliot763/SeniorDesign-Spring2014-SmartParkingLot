import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;

/**
 * Needs to be written.
 * 
 * @author Elliot Dean
 */
public class EntranceController {
	
	EntranceDisplay display;
	XBee xBee;
	XBeeAddress64 CCUAddress = new XBeeAddress64("00 00 00 00 00 00 00 00");
	
	long markerDisplayTime = 7000; // Milliseconds to display space markers
	int nextEntranceId = 0;
	
	/**
	 * Needs to be written.
	 * 
	 * @throws InterruptedException
	 * @throws XBeeException 
	 */
	public static void main(String[] args) throws InterruptedException, XBeeException {
		
		final EntranceController controller = new EntranceController();
		//controller.xBee.open("dev/tty/USB0", 9600);
		
		// Creates a separate thread to display the GUI
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                controller.display = EntranceDisplay.createAndShowGUI();
            } // run
        });
		
		// Wait to ensure that EntranceDisplay is fully initialized
		Thread.sleep(500);
		controller.display.addSpace(278, 305);		
		controller.display.addSpace(121, 67);
		controller.display.updateUI();
		
		Timer timer = new Timer();
		
		while (true) {
			
			if (controller.checkVehicleDetected()) {
				
				// Notifies the Central Control Unit and gets space suggestions
				ArrayList<int[]> spaces = controller.getSpaceSuggestions();
				
				// Updates the display with the new markers
				controller.display.clearSpaces();
				for (int[] position : spaces)
					controller.display.addSpace(position[0], position[1]);
				controller.display.updateUI();
				
				// Sets timer for how long to display and cancels current timer
				timer.cancel();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						controller.display.clearSpaces();
						controller.display.updateUI();
					} // run
				}, controller.markerDisplayTime);
			
			} // if - vehicle detected

		} // while - main program loop
		
	} // main

	/**
	 * Creates an EntranceController object and initializes it's XBee.
	 */
	private EntranceController() {
		xBee = new XBee();
	} // EntranceController
	
	/**
	 * Needs to be written.
	 * 
	 * @return true if a vehicle was detected, false if not
	 */
	private boolean checkVehicleDetected() {
		// TODO Auto-generated method stub
		return false;
	} // checkVehicleDetected

	/**
	 * This method notifies the Central Control Unit that a vehicle is entering
	 * this lot and then waits for the CCU to send back a message containing 
	 * the coordinates of the suggested spaces. Once this message is received
	 * the coordinates are packed into an ArrayList and then returned so that
	 * they may be marked on the display.
	 * 
	 * @return a list of parking space coordinates to be marked on the display
	 */
	private ArrayList<int[]> getSpaceSuggestions() {

		// Creates the "Vehicle Entering" message and updates the counter
		ZNetTxRequest message = new ZNetTxRequest(this.CCUAddress, 
				new int[]{'E', this.nextEntranceId});
		this.nextEntranceId++;
		if (this.nextEntranceId > 255)
			this.nextEntranceId = 0;
		
		// Sends the message to the CCU, ensuring delivery through ACK's
		while(true) {
			try {
				ZNetTxStatusResponse response = (ZNetTxStatusResponse)this.xBee
						.sendSynchronous(message, 3000);
				if (response.isSuccess())
					break;
				else
					throw new XBeeException();
			} catch (XBeeException e) {
				continue; // Message failed, try again
			} // try-catch
		} // while - trying to send the message
		
		// Receives the list of space coordinates and packs them into the list
		ArrayList<int[]> suggestions = new ArrayList<int[]>();;
		while (true) {
			try {
				XBeeResponse response = this.xBee.getResponse();
				if (response.getApiId() == ApiId.ZNET_RX_RESPONSE) {
					ZNetRxResponse rxResponse = (ZNetRxResponse)response;
					if (rxResponse.getData()[0] == 'D') {
						for (int i = 1; i < rxResponse.getData().length; i+=2)
							suggestions.add(new int[]{rxResponse.getData()[i], 
									rxResponse.getData()[i + 1]});
						break;
					} // if - correct response
				} // if - correct ApiId
			} catch (XBeeException e) {
				System.out.println("Error retreiving space coordinates");
			} // try-catch
		} // while - trying to receive space coordinates
		
		return suggestions;
	} // getSpaceSuggestions
	
} // EntranceController - Class
