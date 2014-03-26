
/**
 * 
 * 
 * @author Elliot Dean
 */
public abstract class LotEntity {

	/** The x position of the lot entity from an administrator chosen origin */
	private final int x;
	
	/** The y position of the lot entity from an administrator chosen origin */
	private final int y;
	
	/** The identification number of the lot entity*/
	private final String id;
	
	/**
	 * Sets the coordinates and identification number of a lot entity.
	 * 
	 * @param x: The x coordinate of the lot entity
	 * @param y: The y coordinate of the lot entity
	 * @param id: The identification number of the lot entity
	 */
	public LotEntity(int x, int y, String id) {
		this.x = x;
		this.y = y;
		this.id = id;
	} // LotEntity
	
	/**
	 * Returns the distance between this lot entity and the lot entity passed
	 * as a parameter (e)
	 * 
	 * @param e: A lot entity to find the distance from
	 * @return the distance between this entity and the entity parameter e
	 */
	protected double distance(LotEntity e) {
		return Math.sqrt((this.x - e.getX()) * (this.x - e.getX()) 
				+ (this.y - e.getY()) * (this.y - e.getY()));
	} // distance

	/**
	 * Gets the x coordinate of this lot entity from the lot origin.
	 * 
	 * @return the x value of the entity's coordinates
	 */
	public int getX() {
		return this.x;
	} // getX

	/**
	 * Gets the y coordinate of this lot entity from the lot origin.
	 * 
	 * @return the y value of the entity's coordinates
	 */
	public int getY() {
		return this.y;
	} // getY
	
	/**
	 * Gets the identification number of this lot entity.
	 * 
	 * @return the identification number of this lot entity
	 */
	public String getId()	{
		return this.id;
	} // getId
	
} // LotEntity - Class
