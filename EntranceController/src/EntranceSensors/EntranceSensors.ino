/*
 * This program will continually poll two ultrasonic sensors to check for a ve
 */

const int s1In = 2, s1Out = 3; // Pins for the first sensor
const int s2In = 4, s2Out = 5; // Pins for the second sensor
const int piOut = 12, piIn = 13; // Pins to communicate with the Raspberry Pi

long distanceLimit = 20; // Minimum detection distance in millimeters

void setup() {
  
  // Set states of the io pins
  pinMode(s1In, INPUT);
  pinMode(s2In, INPUT);
  pinMode(piIn, INPUT);
  pinMode(s1Out, OUTPUT);
  pinMode(s2Out, OUTPUT);
  pinMode(piOut, OUTPUT);
  digitalWrite(s1Out, LOW);
  digitalWrite(s2Out, LOW);
  digitalWrite(piOut, LOW);
  
} // setup

void loop() {
  
  if (checkSensor(s1Out, s1In) && !checkSensor(s2Out, s2In)) {
    long startTime = millis();
    while (millis() - startTime < 5000) {
      if (checkSensor(s2Out, s2In) && checkSensor(s1Out, s1In)) {

        sendNotification();
        
        // Wair a few seconds before checking again so repeat messages aren't sent
        long startTime = millis();
        while (millis() - startTime < 3000) ;  
        
        break;
      } // if - vehicle has entered
      else if (!checkSensor(s1Out, s1In)) {
        break;
      } // else if - likely not a vehicle
    } // while - not timed out
  } // if - something is in front of the first sensor but not the second

} // loop

/*
 * Needs to be written
 */
boolean checkSensor(int outPin, int inPin) {
  
  // Set off the trigger pin
  long startTime = micros();
  digitalWrite(outPin, LOW);
  while (micros() - startTime < 2) ; // Set low for 2 microseconds for clean start
  digitalWrite(outPin, HIGH);
  startTime = micros();
  while (micros() - startTime < 10) ; // Set trigger high for 10 microseconds
  digitalWrite(outPin, LOW);
  
  // Time the echo pulse
  long pulse = pulseIn(inPin, HIGH);
  
  // Return true if distance is less than minimum detection distance, false otherwise
  if ((pulse / 58) < distanceLimit) return true;
  else return false;
  
} // checkSensor

/*
 * Needs to be written
 */
void sendNotification() {
  digitalWrite(piOut, HIGH);
  while (!digitalRead(piIn)) ; // Wait for acknowledgement
  digitalWrite(piOut, LOW);
} // sendNotification
