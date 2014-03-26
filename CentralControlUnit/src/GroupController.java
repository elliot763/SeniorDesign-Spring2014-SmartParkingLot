import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * 
 * @author Elliot Dean
 */
public class GroupController extends LotEntity {
	
	private final XBeeAddress64 address64;
	private XBeeAddress16 address16;
	
	/**
	 * Creates an object that represents a physical group controller.
	 * 
	 * @param x: The x coordinate of the group controller
	 * @param y: The y coordinate of the group controller
	 * @param id: The group controllers identification number
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
	
	public XBeeAddress64 getAddress64() {
		return this.address64;
	} // getAddress64
	
	public XBeeAddress16 getAddress16() {
		return this.address16;
	} // getAddress16
	
	public void setAddress16(XBeeAddress16 address) {
		this.address16 = address;
	} // setAddress16
	
} // GroupController - Class
