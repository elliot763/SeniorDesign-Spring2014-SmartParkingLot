import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;

/**
 * The EntranceController class allows a Raspberry Pi, along with an XBee radio
 * and two ultrasonic sensors, to be used as an entrance controller in a Smart
 * Parking Lot. When executed, a map of the lot will be displayed full screen
 * and will update with space suggestions for each lot destination each time a
 * vehicle is detected entering the lot at this entrance. The path to the
 * serial port that the XBee is connected to should be supplied as a runtime
 * parameter.
 * 
 * @author Elliot Dean
 */
public class EntranceController {
	
	// Objects used to communicate with the Arduino/Entrance Sensors
	final GpioController gpio = GpioFactory.getInstance();
	final GpioPinDigitalOutput arduinoOut = gpio.provisionDigitalOutputPin(
			RaspiPin.GPIO_02, PinState.LOW);
	final GpioPinDigitalInput arduinoIn = gpio.provisionDigitalInputPin(
			RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);
	
	EntranceDisplay display;
	XBee xBee;
	
	int controllerId = 0; // The entrance number
	int nextEntranceId = 0;
	long markerDisplayTime = 7000; // Milliseconds to display space markers
	
	/**
	 * The program will begin by initializing the necessary objects and
	 * starting the EntranceDisplay in a separate thread. It then enters the
	 * main program loop which will continually check if a vehicle is entering
	 * the lot and if one is detected it will notify the Central Control Unit
	 * which will then return a list of coordinates for each suggestion. Those
	 * coordinates will then be indicated on the display for a set amount of
	 * time or until another vehicle is detected.
	 * 
	 * @throws InterruptedException
	 * @throws XBeeException 
	 */
	public static void main(String[] args) 
			throws InterruptedException, XBeeException {
		
		final EntranceController controller = new EntranceController();
		controller.xBee.open(args[0], 9600);
		
		// Creates a separate thread to display the GUI
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                controller.display = EntranceDisplay.createAndShowGUI();
            } // run
        });
		
		// Wait to ensure that EntranceDisplay is fully initialized
		Thread.sleep(10000);
		
		Timer timer = new Timer();
		
		// The main program loop
		while (true) {
			
			if (controller.checkVehicleDetected()) {
				
				// Notifies the Central Control Unit and gets space suggestions
				ArrayList<int[]> spaces = controller.getSpaceSuggestions();
				
				if (spaces.size() > 0) {
					
					// Updates the display with the new markers
					controller.display.clearSpaces();
					for (int[] position : spaces)
						controller.display.addSpace(position[0], position[1]);
					controller.display.updateUI();

					// Sets a timer for how long to display the suggestions
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							controller.display.clearSpaces();
							controller.display.updateUI();
						} // Time out actions
					}, controller.markerDisplayTime);
					
				} // if - spaces are available
				
				else {
					
					// Displays a message for a specified time
					controller.display.displayLotFullMessage();
					timer.schedule(new TimerTask() {
						public void run() {
							controller.display.clearLotFullMessage();
						} // Time out actions
					}, controller.markerDisplayTime);
					
				} // else - lot full
				
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
		ZNetTxRequest message = new ZNetTxRequest(
				XBeeAddress64.ZNET_COORDINATOR, new int[]{
						'E', this.nextEntranceId, this.controllerId});
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
						for (int i = 1; i < rxResponse.getData().length; i+=4)
							suggestions.add(new int[]{
									(rxResponse.getData()[i] << 8) + 
									rxResponse.getData()[i + 1], 
									(rxResponse.getData()[i + 2] << 8) +
									rxResponse.getData()[i + 3]});
						break;
					} // if - correct response
				} // if - correct ApiId
			} catch (XBeeException e) {
				System.out.println("Error retreiving space coordinates");
			} // try-catch
		} // while - trying to receive space coordinates
		
		return suggestions;
	} // getSpaceSuggestions
	
	/**
	 * This method waits for a vehicle to enter the lot, at which point the 
	 * input pin from the arduino will become HIGH. Once this happens it will
	 * pulse the output pin to the arduino HIGH for a short amount of time to
	 * let the arduino know that it received the notification so that it will
	 * return the input pin to it's LOW state. It then returns true.
	 * 
	 * @return true only when a vehicle is detected
	 */
	private boolean checkVehicleDetected() {

		while (arduinoIn.isLow()) ; // Wait for a notification
		
		// Acknowledge the notification by pulsing the output pin HIGH
		arduinoOut.setState(PinState.HIGH);
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < 200) ;
		arduinoOut.setState(PinState.LOW);
		return true;
		
	} // checkVehicleDetected
	
} // EntranceController - Class
