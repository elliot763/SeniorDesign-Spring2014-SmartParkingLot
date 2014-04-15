#include <XBee.h>

/*
 * This program allows an Arduino board to act as a group controller in a Smart
 * Parking Lot. It controls several ultrasonic sensors that each monitor a
 * single parking space, an indicator (light) that shows the current state of
 * the parking spaces in it's area, and communicates with the lot's central
 * control unit through a XBee radio.
 *
 * Authors: Kaya Abe, Elliot Dean
 */

// Variables used to tune the operation of the Group Controller
long distanceLimit = 30; // Maximum distance before an object is detected
long maxReservationTime = 20 * 1000; // Time before reservations time out
long minDetectionTime = 4000; // Amount of time before a detection is processed

// Area Indicator pins
const int green = 13;
const int yellow = 12;

/*
 * These variables are used to keep track of the state of each parking space
 * sensor. The only variable that must be set up manually is the sensors array
 * which specifies which pins each sensor is connected to (e.g. initializing
 * the array to {3, 4, 5, 6} would mean that there are separate space sensors
 * connected at digital IO pins 3, 4, 5, and 6. Note that the index that a pin
 * is placed at directly corresponds to it's space number that is held within
 * the Central Control Unit's configuration file, so in the above example the
 * sensor connected to pin 3 would correspond to parking space GX.0 and the
 * one at pin 6 would correspond to parking space GX.3.
 */
const int sensors[] = {3};
const int numberOfSensors = sizeof(sensors) / sizeof(int);
boolean reserved[numberOfSensors];
boolean spaceAvailable[numberOfSensors];
long reservationTime[numberOfSensors];

// Variables used for XBee communication
XBee xbee = XBee();
uint8_t payload[] = {'S', 0, 'A'};
XBeeAddress64 CCU = XBeeAddress64(0x0, 0x0);
ZBRxResponse rx = ZBRxResponse();
ZBTxRequest tx = ZBTxRequest(CCU, payload, sizeof(payload));
ZBTxStatusResponse txStatus = ZBTxStatusResponse();

/*
 * Sets up the XBee object to be used to communicate with the Central Control
 * Unit and initializes each of the parking space status arrays.
 */
void setup() {
	Serial.begin(9600);
	xbee.setSerial(Serial);

	pinMode(green, OUTPUT);
	pinMode(yellow, OUTPUT);

	for (int i = 0; i < numberOfSensors; i++) {
		reserved[i] = false;
		spaceAvailable[i] = true;
		reservationTime[i] = 0l;
	} // for - initialize arrays
} // setup

/*
 * The main program loop for the Group Controller will continually check for
 * and respond to messages from the Central Control Unit, check for any changes
 * in state of any of it's space sensors and notify the Central Control Unit if
 * a change has occurred, check if any space reservations have timed out and
 * notify the Central Control Unit that those spaces are now available if they
 * have, and finally it updates it's area indicators based on the current state
 * of each of it's spaces.
 */
void loop() {
	checkMessages();
	checkSpaces();
	checkReservationTimes();
	updateIndicators();
} // loop

/*
 * This method gets the current availability state of each of the parking
 * spaces. If the states of any space has changed, and remains changed for the
 * minimum amount of time, the state of that space is updated and it's new
 * state is sent to the central control unit
 */
void checkSpaces() {

	boolean wasAvailable, isAvailable;
	for (int i = 0; i < numberOfSensors; i++) {

		wasAvailable = spaceAvailable[i];
		isAvailable = (checkDistance(sensors[i]) > distanceLimit);

		if (isAvailable != wasAvailable) {

			long startTime = millis();
			while (millis() - startTime < minDetectionTime) ; // Wait

			if (isAvailable == (checkDistance(sensors[i]) > distanceLimit)) {
				spaceAvailable[i] = isAvailable;
				sendUpdate(i, isAvailable);
				if (reserved[i] && !isAvailable)
					reserved[i] = false;
			} // if - double check

		} // if - state changed

	} // for - each sensor

} // checkSpaces

/*
 * This method checks if any of the reservations have been set too long. If any
 * have, they are cleared and their updated state is sent to the central
 * control unit.
 */
void checkReservationTimes() {
	for (int i = 0; i < numberOfSensors; i++) {
		if (reserved[i] && millis()-reservationTime[i] >= maxReservationTime) {
			reserved[i] = false;
			sendUpdate(i, true);
		} // if - reservation time up
	} // for -
} // checkReservationTimes

/*
 * This method updates the state of the indicator based on the current state of
 * each space. A yellow light is shown if there are any reserved spaces, green
 * if there are any available spaces but none reserved, or no light otherwise.
 */
void updateIndicators() {

	// Turn light yellow if there are any reservations
	for (int i = 0; i < numberOfSensors; i++) {
		if (reserved[i]) {
			turnOnSemaphore(HIGH, LOW);
			return;
		} // if - space reserved
	} // for - check reservations

	// Turn light green is any space is available
	for (int i = 0; i < numberOfSensors; i++) {
		if (spaceAvailable[i]) {
			turnOnSemaphore(LOW, HIGH);
			return;
		} // if - space is available
	} // for - check availability

	// If no reserved or available spaces were found
	turnOnSemaphore(LOW, LOW);

} // updateIndicators

/*
 * This method sends out a short pulse to an ultrasonic sensor at the given pin
 * and times how long the resulting input pulse lasts. It then converts that
 * time to a distance and returns the distance.
 */
long checkDistance(int pingPin) {

	long pulse, cm;
	long startTime;

	// Sends a short pulse to the ultrasonic sensor
	pinMode(pingPin, OUTPUT);
	digitalWrite(pingPin, LOW);
	startTime = micros();
	while (micros() - startTime < 2) ; // Wait 2 microSeconds
	digitalWrite(pingPin, HIGH);
	startTime = micros();
	while (micros() - startTime < 5) ; // Wait 5 microseconds
	digitalWrite(pingPin, LOW);

	// Times the resulting input pulse from the ultrasonic sensor
	pinMode(pingPin, INPUT);
	pulse = pulseIn(pingPin, HIGH);

	// Returns the distance
	cm = distance(pulse);
	return cm;

} // checkDistance

/*
 * This method converts the duration of an input pulse from an ultrasonic
 * sensor to an approximate distance in centimeters.
 */
long distance(long time) {
	return time/58;
} // distance

/*
 * Used for controlling the RGB LED for testing.
 */
void turnOnSemaphore(int y, int g) {
	digitalWrite(yellow, y);
	digitalWrite(green, g);
} // turnOnSemaphore

/*
 * This method reads in any available messages from the group controller's
 * XBee radio and performs the correct actions based on the type of message
 * that is received. The expected message types are as follows:
 *
 * "Reservation Request" - The specified space should be reserved for a set
 * amount of time.
 * 		- Byte 0: 'R'
 * 		- Byte 1: The space number
 */
void checkMessages() {

	while (true) {

		xbee.readPacket();

		if (xbee.getResponse().isAvailable()) {
			if (xbee.getResponse().getApiId() == ZB_RX_RESPONSE) {

				xbee.getResponse().getZBRxResponse(rx);
				if (rx.getData(0) == 'R') {
					reserved[rx.getData(1)] = true;
					reservationTime[rx.getData(1)] = millis();
				} // if - Reservation request message

			} // if - Series 2 RX response
		} // if - message to parse

		else return; // no more packets

	} // while - packets to parse

} // checkMessages

/*
 * This method sends a "Space Update" message to the Central Control Unit,
 * specifying that the given space has become available or occupied based on
 * the given boolean value. The structure of the message is as follows:
 * 		- Byte 0: 'S'
 * 		- Byte 1: The space number
 * 		- Byte 2: 'A' if the space is available, 'O' otherwise
 */
void sendUpdate(uint8_t spaceNumber, boolean isAvailable) {

	while (true) {

		// Send the message with the supplied information
		payload[1] = spaceNumber;
		payload[2] = (isAvailable)?'A':'O';
		xbee.send(tx);

		// Wait for response
		if (xbee.readPacket(500)) {

			if (xbee.getResponse().getApiId() == ZB_TX_STATUS_RESPONSE) {

				xbee.getResponse().getZBTxStatusResponse(txStatus);
				if (txStatus.getDeliveryStatus() == SUCCESS) return;
				else continue;
			} // if - status response received
		} // if - packet received
	} // while - trying to send update
} // sendUpdate

