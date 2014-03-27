import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;

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
	XBee xBee;
	
	/**
	 * The main program that will run while the Smart Lot is active. It begins
	 * by reading a text file with the required information for all of the lot
	 * entities. It uses this information to instantiate all of the lot 
	 * components as well as its wireless communication device.
	 * 
	 * The main program loop will:
	 * 1) Check for input to allow entrance into administrative control mode
	 * 2) Listen for input from wireless device.
	 * 		2.1) If input is received from a group controller: 
	 * 			2.1.1) Update the applicable variables for the ParkingSpace 
	 * 			objects referenced by the group controller.
	 * 			2.1.2) Check if any newly available spaces are closer to any 
	 * 			Destination than those Destination's previous bestSpace and
	 * 			change that Destination's bestSpace if so.
	 * 		2.2) If input is received from an entrance controller:
	 * 			2.2.1) Send information about the current bestSpace for each 
	 * 			Destination back to the entrance controller.
	 * 			2.2.2) Send reservation requests to each of GroupController
	 * 			that one or more of the ParkingSpaces belongs to.
	 * 			2.2.3) Set the state of each of the ParkingSpace objects to 
	 * 			occupied.
	 * 			2.2.4) Update the bestSpace variable for each Destination.
	 * 3)
	 * @throws IOException 
	 * @throws XBeeException 
	 */
	public static void main(String[] args) throws IOException, XBeeException {
		
		CentralControlUnit CCU = new CentralControlUnit();
		CCU.initialize("SmartLot.txt");
//		CCU.xBee.open("COMX", 9600);

		for (Destination dest : CCU.destinations) {
			System.out.println("Destination: " + dest.getId());
			System.out.println("\tX: " + dest.getX() + "\tY: " + dest.getY());
		} // for each - destinations
		
		for (ParkingSpace space : CCU.spaces) {
			System.out.println("Space: " + space.getId());
			System.out.println("\tX: " + space.getX()
					+ "\tY: " + space.getY()
					+ "\tController ID: " + space.getController().getId());
		} // for each - spaces
		
		System.out.println("\nDistance between destination " 
				+ CCU.destinations.get(0).getId() + " and space "
				+ CCU.spaces.get(0).getId() + ": " 
				+ CCU.destinations.get(0).distance(CCU.spaces.get(0)));
		
		CCU.updateBestSpaces();
		for (Destination dest : CCU.destinations)
			System.out.println("Best space for destination " + dest.getId()
					+ ": " + dest.getBestSpace().getId() + " - Distance: "
					+ dest.distance(dest.getBestSpace()));
		
		CCU.checkIfBestSpace(CCU.spaces.get(0));
		
		System.out.println("Test group controller address64: " 
				+ CCU.spaces.get(0).getController().getAddress64());
		
//		while (true) {
//			
//			// Check for input to enter admin control (could be separate thread)
//			
//		} // while - main program loop
		
	} // main
	
	/**
	 * Creates a CentralControlUnit object
	 */
	public CentralControlUnit() {
		destinations = new LinkedList<Destination>();
		spaces = new LinkedList<ParkingSpace>();
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
	
} // CentralControlUnit - Class
