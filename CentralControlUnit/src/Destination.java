
/**
 * The Destination class represents a physical destination within a Smart
 * Parking Lot (e.g. An entrance to a store or business). It has a single 
 * ParkingSpace variable that represents the nearest available parking space to
 * the destination. Since there is no Smart Parking lot hardware that directly
 * related to each lot destination, the only other attributes that are assigned
 * to a destination are its position coordinates.
 * 
 * @author Elliot Dean
 */
public class Destination extends LotEntity{

	/** The closest available parking space to this destination */
	private ParkingSpace bestSpace;
	
	/**
	 * Creates an object that represents a physical destination (e.g. An 
	 * entrance to a store/business)
	 * 
	 * @param x: The destinations x coordinate
	 * @param y: The destinations y coordinate
	 * @param id: The destinations identifier
	 */
	public Destination(int x, int y, String id) {
		super(x, y, id);
	} // Destination
	
	/**
	 * Set's a new best parking space for this destination.
	 * 
	 * @param space: The new closest available space to the destination
	 */
	public void setBestSpace(ParkingSpace space) {
		if (space.isAvailable())
			this.bestSpace = space;
		else
			System.out.println("Error: attempt to set occupied space as best");
	} // setBestSpace
	
	/**
	 * Gets the current closest available parking space to the destination.
	 * 
	 * @return The ParkingSpace object that has been set as best
	 */
	public ParkingSpace getBestSpace() {
		return this.bestSpace;
	} // getBestSpace
	
} // Destination - Class
