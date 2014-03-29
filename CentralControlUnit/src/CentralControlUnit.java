import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;

/**
 * The CentralConrolUnit class represents the central control unit of a Smart
 * Parking Lot and contains all of the necessary functions that allow the 
 * control unit to keep track of changes within the lot and communicate with
 * other lot components. The main method of the CentralControlUnit class first
 * sets up the CentralControlUnit object and other objects for various lot
 * components and also contains the main programming loop that runs the entire 
 * time that the Smart Parking Lot is active.
 * 
 * @author Elliot Dean
 */
public class CentralControlUnit {

	LinkedList<Destination> destinations;
	LinkedList<ParkingSpace> spaces;
	HashMap<XBeeAddress64, GroupController> addressMap;
	XBee xBee;
	
	int[] lastEntranceId = {-1}; // Add -1's for each lot entrance controller
	
	/**
	 * The main program that will run while the Smart Lot is active. It begins
	 * by reading a text file with the required information for all of the lot
	 * entities. It uses this information to instantiate all of the lot 
	 * components as well as its wireless communication device. It then
	 * continuously checks for messages from the XBee radio and performs the 
	 * correct actions when one is received.
	 * 
	 * @throws IOException 
	 * @throws XBeeException 
	 */
	public static void main(String[] args) throws IOException, XBeeException {
		
		CentralControlUnit CCU = new CentralControlUnit();
		CCU.initialize("SmartLot.txt");
//		CCU.xBee.open("COMX", 9600);
		Thread admin = new Thread(CCU.new AdminControl(CCU));
		admin.start();
		
//		while (true) {
//			
//			// Check for input to enter admin control (could be separate thread)
//			XBeeResponse response = CCU.xBee.getResponse();
//			CCU.processResponse(response);
//			
//		} // while - main program loop
		
	} // main
	
	/**
	 * Creates a CentralControlUnit object
	 */
	public CentralControlUnit() {
		destinations = new LinkedList<Destination>();
		spaces = new LinkedList<ParkingSpace>();
		addressMap = new HashMap<XBeeAddress64, GroupController>();
		xBee = new XBee();
	} // CentralControlUnit

	/**
	 * Adds Destination, Group Controller, and ParkingSpace objects to the 
	 * CentralControlUnit based on input from a file.
	 * 
	 * @param fileName: The name of the file with the lot information
	 * @throws IOException
	 */
	private void initialize(String fileName) throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String nextLine;
		
		while ((nextLine = br.readLine()) != null) {
		
			if (nextLine.trim().equals("DESTINATIONS")) {
				while (!(nextLine = br.readLine().trim())
						.equals("END_DESTINATIONS")) {
					if (!nextLine.isEmpty()) {
						
						String[] destParams = nextLine.split(" ");
						this.destinations.add(new Destination(
								Integer.parseInt(destParams[1]), 
								Integer.parseInt(destParams[2]), 
								destParams[0]));
					
					} // if - line not empty
				} // while - still loading destinations
			} // if - loading destinations
			
			else if (nextLine.trim().equals("GROUP_CONTROLLERS")) {
				while (!(nextLine = br.readLine().trim())
						.equals("END_GROUP_CONTROLLERS")) {
					if (!nextLine.isEmpty()) {
						
						String[] gcParams = nextLine.split(" ", 4);
						GroupController controller = new GroupController(
								Integer.parseInt(gcParams[1]), 
								Integer.parseInt(gcParams[2]), 
								gcParams[0], gcParams[3]);
						addressMap.put(
								new XBeeAddress64(gcParams[3]), controller);
						
						while (!(nextLine = br.readLine().trim())
								.equals("END_SPACES")) {
							String[] spaceParams = nextLine.split(" ");
							this.spaces.add(controller.addSpace(
									Integer.parseInt(spaceParams[1]), 
									Integer.parseInt(spaceParams[2]), 
									controller.getId() + "." + spaceParams[0]));
						} // while - adding spaces
					
					} // if - line not empty
				} // while - still loading group controllers/parking spaces
			} // else if - loading group controllers/parking spaces
		
		} // while - not end of file
		br.close();
		
	} // initialize
	
	/**
	 * Finds the current closest available parking space to each of the 
	 * lot destinations.
	 */
	private void updateBestSpaces() {
		
		for (Destination dest : this.destinations) {
			this.updateBestSpaces(dest);
		} // for each - destination
		
	} // updateBestSpaces
	
	/**
	 * Finds the current closest available parking space to a single 
	 * destination.
	 * 
	 * @param dest: The destination who's best space should be found
	 */
	private void updateBestSpaces(Destination dest) {
		
		Double shortestDistance = -1.0;
		ParkingSpace bestSpace = null;
		
		for (ParkingSpace space : this.spaces) {
			if (space.isAvailable()) {
		
				Double distance = dest.distance(space);
				if (distance < shortestDistance 
						|| shortestDistance < 0.0) {
					shortestDistance = distance;
					bestSpace = space;
				} // if - space is closer than previous best
			
			} // if - space is available
		} // for each - space
		
		dest.setBestSpace(bestSpace);
		
	} // updateBestSpaces - single destination
	
	/**
	 * Checks if a newly available space is the best available space for any of
	 * the lot destinations and sets it as the destinations bestSpace if so.
	 * 
	 * @param space: The space to check
	 */
	private void checkIfBestSpace(ParkingSpace space) {
		for (Destination dest : this.destinations)
			if (dest.distance(space) < dest.distance(dest.getBestSpace()))
				dest.setBestSpace(space);
	} // checkIfBestSpace
	
	/**
	 * Takes an XBee response and processes it. The different types of messages
	 * that are expected and their formats are as follows:
	 * 
	 * Vehicle detected at an entrance:
	 * 		First integer = 'E'
	 * 		Second integer = message identifier (this is in place so that extra
	 * reservations are not made if an ACK from the Central Control Unit to the
	 * Entrance Controller is lost, causing the Entrance Controller to re-send
	 * 'E' message. It is an integer between 0 and 255 and each Entrance 
	 * Controller has it's own counter.)
	 * 		- When this message is received, the coordinates of each
	 * destinations best space will be sent back to the Entrance Controller
	 * and each of these spaces will be set to not available. Then a
	 * reservation request message will be sent to each of those spaces Group
	 * Controllers and new best spaces will be found for each of the
	 * destinations.
	 * 
	 * Space status update:
	 * 		First integer = 'S'
	 * 		Second integer = the space number
	 * 		Third integer = 'A' if space is available, 'O' otherwise
	 * 		- When this message is received, the space with the given space 
	 * number under the Group Controller that sent the message will be updated
	 * to the specified state.
	 * 
	 * @param response: The XBee response object received from the radio.
	 */
	private void processResponse(XBeeResponse response) {
		
		if (response.getApiId() == ApiId.ZNET_RX_RESPONSE) {
			
			ZNetRxResponse rxResponse = (ZNetRxResponse)response;
			if (rxResponse.getData()[0] == 'E') {
				
				int entranceId = rxResponse.getData()[1];
				int entranceController = rxResponse.getData()[2];
				
				if (entranceId > this.lastEntranceId[entranceController]) {
					
					// Update the lastEntranceId variable for the controller
					this.lastEntranceId[entranceController]++;
					if (entranceId >= 255)
						this.lastEntranceId[entranceController] = -1;
					
					ParkingSpace[] reservedSpaces = this.sendBestSpaces(
							rxResponse.getRemoteAddress64());
					this.sendReservationRequests(reservedSpaces);
					this.updateBestSpaces();
					
				} // if - not a repeat message
				
			} // if - vehicle detected at entrance
			
			else if (rxResponse.getData()[0] == 'S') {
				
				// Finds the correct parking space object
				ParkingSpace updatedSpace = null;
				for (ParkingSpace space : this.spaces)
					if (space.getId().equals(this.addressMap.get(
							rxResponse.getRemoteAddress64()).getId() 
							+ "." + rxResponse.getData()[1]))
						updatedSpace = space;
				
				// Changes the state of the space if it was found
				if (updatedSpace == null)
					System.out.println("Error: Unable to find updated space");
				else {
					if (rxResponse.getData()[2] == 'A') {
						updatedSpace.setAvailable(true);
						this.checkIfBestSpace(updatedSpace);
					} // if - space became available
					else if (rxResponse.getData()[2] == 'O') {
						updatedSpace.setAvailable(false);
						for (Destination dest : this.destinations) {
							if (dest.getBestSpace() == updatedSpace)
								this.updateBestSpaces(dest);
						} // for - check if space was any destination's best
					} // else if - space became occupied
				} // else - space found and updated
					
			} // else if - space status update
			
			else {
				System.out.println("Unknown packet received");
			} // else - error
			
		} // if - RX response
		
		else {
			System.out.println("Error: unexpected ApiId");
		} // else - error
		
	} // processResponse
	
	/**
	 * This method takes each destinations best space, sets them as not 
	 * available and sends their coordinates to the supplied address in a
	 * "Display spaces" message. The format of this message type is:
	 * 		First integer = 'D'
	 * 		Second integer = x coordinate 0
	 * 		Third integer = y coordinate 0
	 * 		Fourth integer = x coordinate 1
	 * 		...	
	 * 		Last integer = y coordinate n
	 * 
	 * It then returns all of the ParkingSpace objects that were best spaces.
	 * 
	 * @param dest: The address to send the message to
	 * @return an array of ParkingSpace objects that were the best spaces
	 */
	private ParkingSpace[] sendBestSpaces(XBeeAddress64 address) {
		
		ParkingSpace[] bestSpaces = new ParkingSpace[this.destinations.size()];
		for (int i = 0; i < this.destinations.size(); i++) {
			bestSpaces[i] = this.destinations.get(i).getBestSpace();
			bestSpaces[i].setAvailable(false);
		} // for - put each best space in array to be returned
		
		int[] payload = new int[bestSpaces.length*2 + 1];
		payload[0] = 'D';
		for (int i = 0; i < bestSpaces.length; i++) {
			payload[i*2 + 1] = bestSpaces[i].getX();
			payload[i*2 + 2] = bestSpaces[i].getY();
		} // for - add coordinates to the payload
		
		ZNetTxRequest message = new ZNetTxRequest(address, payload);
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
		
		return bestSpaces;
	} // sendBestSpaces
	
	/**
	 * This method takes in an array of ParkingSpace objects and sends a
	 * "Reservation Request" message to each of their Group Controllers. The
	 * format of this message type is as follows:
	 * 		First integer = 'R'
	 * 		Second integer = space number
	 * 
	 * @param spaces: the spaces to be reserved
	 */
	private void sendReservationRequests(ParkingSpace[] spaces) {
		
		for (ParkingSpace space : spaces) {
			XBeeAddress64 address = space.getController().getAddress64();
			ZNetTxRequest message = new ZNetTxRequest(address, new int[] {'R', 
					Integer.parseInt(space.getId().substring(
							space.getId().lastIndexOf('.')))});
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
			
		} // for - send reservation message to each spaces controller
		
	} // sendReservationRequests
	
	private class AdminControl implements Runnable {

		CentralControlUnit CCU;
		
		AdminControl(CentralControlUnit CCU) {
			this.CCU = CCU;
		} // AdminControl
		
		@Override
		public void run() {
				Scanner keyboard = new Scanner(System.in);
				System.out.println("Welcome to the Smart Parking Lot "
						+ "administrative control panel. (type h for help)");
			while (true) {
				String input = keyboard.nextLine();
				if (input.equalsIgnoreCase("h")) {
					System.out.println("'D': Show destination info");
					System.out.println("'S': Show parking space info");
					System.out.println("'Q': Exit administrative control");
				} // if - help menu
				else if (input.equalsIgnoreCase("D")) {
					for (Destination dest : CCU.destinations) {
						System.out.println("Destination: " + dest.getId());
						System.out.println("\tX: " + dest.getX() + "\tY: " 
						+ dest.getY());
					} // for each - destinations
				} // else if - Destination info
				else if (input.equalsIgnoreCase("S")) {
					for (ParkingSpace space : CCU.spaces) {
						System.out.println("Space: " + space.getId());
						String available = (space.isAvailable() ? 
								"Available" : "Occupied");
						System.out.println("\tX: " + space.getX()
								+ "\tY: " + space.getY()
								+ "\tController ID: " 
								+ space.getController().getId()
								+ "\t" + available);
					} // for each - spaces
				} // else if - Space info
				
				else if (input.equals("Q")) 
					break;
			} // while - true
			keyboard.close();
		}
		
	} // adminControl
	
} // CentralControlUnit - Class
