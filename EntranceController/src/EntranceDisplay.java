import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * The EntranceDisplay class displays a full screen image of a map of a Smart
 * Parking Lot. It's addSpace and clearSpaces methods can be used to control
 * the drawing of space suggestion markers on top of the map. The coordinates
 * used to add space markers can be measured from the top-left corner of the
 * source image in pixels.
 * 
 * @author Elliot Dean
 */
@SuppressWarnings("serial")
public class EntranceDisplay extends JPanel {
	
	/** Object used for full screen rendering */
	static GraphicsDevice device = GraphicsEnvironment
	        .getLocalGraphicsEnvironment().getScreenDevices()[0];
	/** The map of the parking lot */
	private BufferedImage map;
	/** The map, scaled so that it's largest dimension fits the screen */
	private Image scaledMap;
	/** The dimensions of the original map */
	private int mapHeight, mapWidth;
	/** The dimensions and position of the scaled map */
	private int scaledMapWidth, scaledMapHeight, mapTopEdge, mapLeftEdge;
	/** The list of spaces to be drawn */
	private ArrayList<int[]> spaces;
	/** An image to be shown when the lot is full */
	private Image lotFullImage;
	/** Whether the lot full message should be drawn */
	private boolean displayLotFull = false;
	
	/**
	 * This method is used to set the dimensions of the map image and
	 * initialize the list of spaces.
	 * 
	 * @param frame: The frame that the image is being displayed in
	 */
	private void initializeDisplay(JFrame frame) {
		
		try {
			
			lotFullImage = ImageIO.read(new File("LotFull.png"));
			map = ImageIO.read(new File("LotTest.png"));
			if (frame.getWidth() <= frame.getHeight()) {
				scaledMap = map.getScaledInstance(frame.getWidth(), -1, 
						Image.SCALE_SMOOTH);
				mapTopEdge = frame.getHeight()/2 - scaledMap.getHeight(null)/2;
				mapLeftEdge = 0;
			} // if - fit to width of screen
			else {
				scaledMap = map.getScaledInstance(-1, frame.getHeight(), 
						Image.SCALE_SMOOTH);
				mapTopEdge = 0;
				mapLeftEdge = frame.getWidth()/2 - scaledMap.getWidth(null)/2;
			} // else - fit to height of screen
			
			mapHeight = map.getHeight();
			scaledMapHeight = scaledMap.getHeight(null);
			mapWidth = map.getWidth();
			scaledMapWidth = scaledMap.getWidth(null);	
			spaces = new ArrayList<int[]>();
		
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} // try-catch
	
	} // initializeDisplay
	
	/**
	 * Adds the coordinates of a space for a marker to be drawn later. These
	 * coordinates will not be drawn on top of the map until the
	 * EntranceDisplay's paint() method is called, ideally through the 
	 * updateUI() method.
	 * 
	 * @param x: The x coordinate of the space, from the map's left edge
	 * @param y: The y coordinate of the space, from the map's top edge
	 */
	public void addSpace(int x, int y) {
		this.spaces.add(new int[]{x, y});
	} // addSpace
	
	/**
	 * Clears any spaces that are set to be marked on the map. This will not
	 * actually remove markers that have already been drawn unless followed by
	 * a call to the EntranceDisplay's paint() method, usually through the
	 * updateUI() method.
	 */
	public void clearSpaces() {
		this.spaces.clear();
	} // clearSpaces
	
	/**
	 * This method will clear any space suggestions that are currently being
	 * displayed and than immediately show a message indicating that the lot
	 * is full.
	 */
	public void displayLotFullMessage() {
		this.clearSpaces();
		this.displayLotFull  = true;
		this.updateUI();
	} // displayLotFullMessage
	
	/**
	 * This method will immediately remove a lot full message.
	 */
	public void clearLotFullMessage() {
		this.displayLotFull = false;
		this.updateUI();
	} // clearLotFullMessage
	
	@Override
	public void paint(Graphics g) {
		super.paintComponents(g);
		
		// Draws the scaled map in the background of the frame
		g.drawImage(scaledMap, mapLeftEdge, mapTopEdge, null);
		
		// Draws each space marker to the frame
		int circleRadius = 10;
		g.setColor(Color.GREEN);
		for (int i = 0; i < spaces.size(); i++)
			g.fillOval(mapLeftEdge + (spaces.get(i)[0] * scaledMapWidth 
					/ this.mapWidth - circleRadius/2), 
					mapTopEdge + (spaces.get(i)[1] * scaledMapHeight
					/ this.mapHeight - circleRadius/2), 
					circleRadius, circleRadius);
		
		if (this.displayLotFull)
			g.drawImage(lotFullImage, 
					scaledMapWidth/2 - lotFullImage.getWidth(null)/2, 
					scaledMapHeight/2 - lotFullImage.getHeight(null)/2, null);
		
	} // paint
	
	/**
	 * This method creates a full-screen window to show on the entrance display
	 * and returns an instance of the EntranceDisplay object so that it can be
	 * accessed by another thread.
	 * 
	 * @return an instance of the EntranceDisplay object
	 */
	public static EntranceDisplay createAndShowGUI() {

		// Creates and sets up the frame
		final JFrame frame = new JFrame("SmartParkingLot");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setFocusable(true);
		frame.setLocation(0, 0);
		frame.setBackground(Color.BLACK);
		frame.setUndecorated(true);
		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					frame.dispose();
			}
		});

		// Creates the EntranceDisplay and adds it to the frame
		EntranceDisplay display = new EntranceDisplay();
		frame.add(display);

		// Shows the frame
		device.setFullScreenWindow(frame);
		frame.pack();
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		frame.setResizable(false);
		
		display.initializeDisplay(frame);
		
		return display;

	} // createAndShowGUI
	
} // EntranceDisplay - Class
