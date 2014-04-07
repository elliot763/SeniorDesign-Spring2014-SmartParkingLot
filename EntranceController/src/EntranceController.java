import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
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
	
	// Objects used to poll the ultrasonic entrance sensors
	final GpioController gpio = GpioFactory.getInstance();
	final GpioPinDigitalMultipurpose s1 = gpio.provisionDigitalMultipurposePin(
			RaspiPin.GPIO_00, PinMode.DIGITAL_OUTPUT);
	final GpioPinDigitalOutput s2Out = gpio.provisionDigitalOutputPin(
			RaspiPin.GPIO_02, PinState.LOW);
	final GpioPinDigitalInput s2In = gpio.provisionDigitalInputPin(
			RaspiPin.GPIO_03, PinPullResistance.PULL_DOWN);
	
	EntranceDisplay display;
	XBee xBee;
	
	int controllerId = 0; // The entrance number
	int nextEntranceId = 0;
	long markerDisplayTime = 7000; // Milliseconds to display space markers
	long thresholdDistance = 20; // Maximum detection distance
	
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
			
			// Time between polling
			Thread.sleep(100);

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
									rxResponse.getData()[i] << 8 + 
									rxResponse.getData()[i + 1], 
									rxResponse.getData()[i + 2] << 8 +
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
	 * This method checks for a vehicle entering the lot by polling the 
	 * entrance controller's sensors and returns true if one is detected or 
	 * false otherwise.
	 * 
	 * @return true only if a vehicle is detected
	 */
	private boolean checkVehicleDetected() {

		if (!checkSensor(s2Out, s2In)) {
			if (checkSensor(s1)) {
				long firstTime = System.currentTimeMillis();
				while (System.currentTimeMillis() - firstTime < 5000) {
					if (checkSensor(s2Out, s2In))
						return true;
				} // while - timeout after 5 seconds
				return false;
			} // if - something at first sensor
			return false;
		} // if - nothing at second sensor
		else
			return false;
	
	} // checkVehicleDetected
	
	/**
	 * This method polls a four pin ultrasonic sensor at the given input and
	 * output pins and returns true if and only if an object is detected within
	 * the threshold distance.
	 * 
	 * @param sensorOutPin: The trigger pin of the four pin ultrasonic sensor
	 * @param sensotInPin: The echo pin of the four pin ultrasonic sensor
	 * @return true only if an object is detected
	 */
	private boolean checkSensor(GpioPinDigitalOutput sensorOutPin, 
			GpioPinDigitalInput sensorInPin) {
		
		// Set the trigger pin low momentarily to ensure clean start
		sensorOutPin.setState(PinState.LOW);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < 2000) ; // Do nothing
		
		// Pulse the trigger pin high for 10 microseconds
		sensorOutPin.setState(PinState.HIGH);
		startTime = System.nanoTime();
		while (System.nanoTime() - startTime < 10000) ; // Do nothing
		sensorOutPin.setState(PinState.LOW);
		
		// Time the resulting pulse at the echo pin
		while (sensorInPin.isLow()) ; // Do nothing until pulse begins
		startTime = System.nanoTime();
		while (sensorInPin.isHigh()) ; // Do nothing until pulse is over
		long endTime = System.nanoTime();
		
		// Return true only if the sensed distance is less than the threshold
		if (distance((endTime - startTime) / 1000) < thresholdDistance)
			return true;
		else
			return false;
		
	} // checkSensor - 4 Pin
	
	/**
	 * This method polls a three pin ultrasonic sensor at the given 
	 * multipurpose pin and returns true if and only if an object is detected 
	 * within the threshold distance.
	 * 
	 * @param sensorPin: The input/output pin of the ultrasonic sensor
	 * @return true only if an object is detected
	 */
	private boolean checkSensor(GpioPinDigitalMultipurpose sensorPin) {
		
		// Set the pin as a digital output
		sensorPin.setMode(PinMode.DIGITAL_OUTPUT);
		
		// Set the pin low momentarily to ensure a clean start
		sensorPin.setState(PinState.LOW);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < 2000) ; // Do nothing
		
		// Pulse the pin high for 5 microseconds
		sensorPin.setState(PinState.HIGH);
		startTime = System.nanoTime();
		while (System.nanoTime() - startTime < 5000) ; // Do nothing
		sensorPin.setState(PinState.LOW);
		
		// Set the pin as a digital input
		sensorPin.setMode(PinMode.DIGITAL_INPUT);
		
		// Time the resulting pulse
		while (sensorPin.isLow()) ; // Do nothing until pulse begins
		startTime = System.nanoTime();
		while (sensorPin.isHigh()) ; // Do nothing until pulse is over
		long endTime = System.nanoTime();
		
		// Return true only if the sensed distance is less than the threshold
		if (distance((endTime - startTime) / 1000) < thresholdDistance)
			return true;
		else
			return false;
		
	} // checkSensor - 3 Pin
	
	/**
	 * This method converts the time of the input pulse from an ultrasonic 
	 * sensor to an approximate distance in centimeters.
	 * 
	 * @param time: The length of the sensor pulse in microseconds
	 * @return
	 */
	private long distance(long time) {
		return time / 29;
	} // distance
	
} // EntranceController - Class
