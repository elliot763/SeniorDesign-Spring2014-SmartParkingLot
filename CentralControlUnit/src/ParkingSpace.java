
/**
 * 
 * @author Elliot Dean
 */
public class ParkingSpace extends LotEntity{
	
	private final GroupController controller;
	private boolean isAvailable;
	
	/**
	 * Creates an object that represents a physical parking space.
	 * 
	 * @param x: The parking space's x coordinate in relation to its controller
	 * @param y: The parking space's y coordinate in relation to its controller
	 * @param id: The identification number of the parking space
	 * @param controller: The controller that the parking space belongs to
	 */
	public ParkingSpace(int x, int y, String id, GroupController controller) {
		super(x + controller.getX(), y + controller.getY(), id);
		this.controller = controller;
		this.isAvailable = true;
	} // ParkingSpace
	
	/**
	 * Gets the GroupController object that represents the physical group 
	 * controller that this parking space is assigned to.
	 * 
	 * @return The GroupController object that the parking space is assigned to
	 */
	public GroupController getController() {
		return this.controller;
	} // getController
	
	/**
	 * Returns whether or not the space is available.
	 * 
	 * @return true if the space is available and false if it is occupied.
	 */
	public boolean isAvailable() {
		return this.isAvailable;
	} // isAvailable
	
	/**
	 * Sets whether or not the space is available.
	 * 
	 * @param state: true if space is available and false otherwise
	 */
	public void setAvailable(boolean state) {
		this.isAvailable = state;
	} // setAvailable
	
} // ParkingSpace - Class
