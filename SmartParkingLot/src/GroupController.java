
/**
 * 
 * @author Elliot Dean
 */
public class GroupController extends LotEntity {
	
	/**
	 * Creates an object that represents a physical group controller.
	 * 
	 * @param x: The x coordinate of the group controller
	 * @param y: The y coordinate of the group controller
	 * @param id: The group controllers identification number
	 */
	public GroupController(int x, int y, String id) {
		super(x, y, id);
	} // GroupController
	
	/**
	 * Creates and returns a new parking space that is handled by this group
	 * controller. Note that the origin of the x and y coordinates is at the
	 * group controller, not the coordinate origin of the lot.
	 *  
	 * @param x: The space's x coordinate in relation to the controller
	 * @param y: The space's y coordinate in relation to the controller
	 * @param id: The identification number of the parking space
	 */
	public ParkingSpace addSpace(int x, int y, String id) {
		return new ParkingSpace(x, y, id, this);
	} // addSpace
	
} // GroupController - Class
