// Do not remove the include below
#include "GroupController.h"

// Note: The "GroupController.h" include, as well as several other files in
// this project, are an artifact of writing this code in Eclipse (in order to
// simplify version control). This file may be saved as a standard .ino file
// and the rest of the files (other than necessary dependencies) can be removed
// when loading the program on to an Arduino.

/*
* This program controls allows an Arduino board to act as a group controller
* in a Smart Parking Lot. It controls several ultrasonic sensors that each
* monitor a single parking space, an indicator (light) that shows the current
* state of the parking spaces in it's area, and communicates with the lot's
* central control unit through a XBee radio.
*
* Authors: Kaya Abe, Elliot Dean
*/

const int green = 13;
const int yellow = 12;
const int sensors[] = {3, 4, 5};

boolean reserved[sizeof(sensors) / sizeof(int)];
boolean spaceAvailable[sizeof(sensors) / sizeof(int)];
long reservationTime[sizeof(sensors) / sizeof(int)];

long distanceLimit = 20;
long maxReservationTime = 60 * 1000;
long minDetectionTime = 4000;

void setup()
{
  Serial.begin(9600);

  pinMode(green, OUTPUT);
  pinMode(yellow, OUTPUT);

  for (int i = 0; i < sizeof(sensors) / sizeof(int); i++) {
	  reserved[i] = false;
	  spaceAvailable[i] = true;
	  reservationTime = 0l;
  } // for - initialize arrays
}

void loop()
{
  checkMessages();
  checkSpaces();
  checkReservationTimes();
  updateIndicators();
}

/*
 * This method gets the current availability state of each of the parking
 * spaces. If the states of any space has changed, and remains changed for the
 * minimum amount of time, the state of that space is updated and it's new
 * state is sent to the central control unit
 */
void checkSpaces() {

	boolean wasAvailable, isAvailable;
	for (int i = 0; i < sizeof(sensors)/sizeof(int); i++) {

		wasAvailable = spaceAvailable[i];
		if (checkDistance(sensors[i]) <= distanceLimit)
			isAvailable = false;
		else
			isAvailable = true;

		if (isAvailable != wasAvailable) {

			long startTime = millis();
			while (millis() - startTime < minDetectionTime) ; // Wait 4 seconds

			if (isAvailable == (checkDistance(sensors[i]) <= distanceLimit)) {
				sendUpdate(i, isAvailable);
				if (reserved[i])
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
	for (int i = 0; i < sizeof(sensors) / sizeof(int); i++) {
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
	for (int i = 0; i < sizeof(sensors) / sizeof(int); i++) {
		if (reserved[i]) {
			turnOnSemaphore(HIGH, LOW);
			return;
		} // if - space reserved
	} // for - check reservations

	// Turn light green is any space is available
	for (int i = 0; i < sizeof(sensors) / sizeof(int); i++) {
		if (spaceAvailable[i]) {
			turnOnSemaphore(LOW, HIGH);
			return;
		} // if - space is available
	} // for - check availability

	// If no reserved or available spaces were found
	turnOnSemaphore(LOW, LOW);

} // updateIndicators

// working!
long checkDistance(int pingPin)
{
  long pulse, cm;

  long startTime;

  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  startTime = micros();
  while (micros() - startTime < 2) ; // Wait 2 microSeconds
  digitalWrite(pingPin, HIGH);
  startTime = micros();
  while (micros() - startTime < 5) ; // Wait 5 microseconds
  digitalWrite(pingPin, LOW);

  /*
  // Used for 4-pin sensor
  digitalWrite(trigPin, LOW);
  startTime = micros();
  while (micros() - startTime < 2) ; // Wait 2 microseconds
  digitalWrite(trigPin, HIGH);
  startTime = micros()
  while (micros() - startTime < 10) ; // Wait 10 microseconds
  digitalWrite(trigPin, LOW);
  */

  pinMode(pingPin, INPUT);
//  pulse = pulseIn(echoPin, HIGH); // used for 4-pin sensor
  pulse = pulseIn(pingPin, HIGH);
  cm = distance(pulse);
  return cm;
}

long distance(long time)
{
  return time/29/2;
}

void turnOnSemaphore(int y, int g)
{
  digitalWrite(yellow, y);
  digitalWrite(green, g);
}

/*
 * Needs to be written.
 */
void checkMessages() {

} // checkMessages

/*
 * Needs to be written
 */
void sendUpdate(int spaceNumber, boolean isAvailable) {

} // sendUpdate
