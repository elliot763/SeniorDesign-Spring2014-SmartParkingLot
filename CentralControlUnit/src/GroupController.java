import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * The GroupController class represents a physical group controller within a
 * Smart Parking Lot. Since each group controller has it's own XBee radio with
 * a unique 64-bit address, the class stores this address which must be passed
 * to the constructor in order to communicate with the radio. The class also
 * has a method that allows ParkingSpace objects to be added to it which
 * represent the parking space sensors that are attached to the physical group
 * controller. The class extends the LotEntity abstract class and therefore
 * has position coordinates which can be accessed.
 * 
 * @author Elliot Dean
 */
public class GroupController extends LotEntity {
	
	private final XBeeAddress64 address64;
	
	/**
	 * Creates an object that represents a physical group controller.
	 * 
	 * @param x: The x coordinate of the group controller
	 * @param y: The y coordinate of the group controller
	 * @param id: The group controllers identifier
	 */
	public GroupController(int x, int y, String id, String address) {
		super(x, y, id);
		this.address64 = new XBeeAddress64(address);
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
	
	/**
	 * Returns the 64-bit address of this group controller's XBee radio.
	 * 
	 * @return the 64-bit address of the XBee
	 */
	public XBeeAddress64 getAddress64() {
		return this.address64;
	} // getAddress64
	
} // GroupController - Class
