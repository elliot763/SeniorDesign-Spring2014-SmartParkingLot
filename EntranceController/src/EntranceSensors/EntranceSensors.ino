/*
 * This program will continually poll two ultrasonic sensors to check for a vehicle
 * entering the parking lot. Once a vehicle is detected it will notify it's entrance
 * controller by setting a single pin HIGH until the entrance controller acknowledges
 * that it has received the notification.
 */

const int s1In = 2, s1Out = 3; // Pins for the first sensor
const int s2In = 4, s2Out = 5; // Pins for the second sensor
const int piOut = 12, piIn = 13; // Pins to communicate with the Raspberry Pi

long distanceLimit = 20; // Minimum detection distance in centimeters
long detectionTime = 5000; // Time to check sensor 2 after the first is triggered

void setup() {
  
  // Set the modes of the io pins
  pinMode(s1In, INPUT);
  pinMode(s2In, INPUT);
  pinMode(piIn, INPUT);
  pinMode(s1Out, OUTPUT);
  pinMode(s2Out, OUTPUT);
  pinMode(piOut, OUTPUT);
  
  // Set the initial state of the output pins
  digitalWrite(s1Out, LOW);
  digitalWrite(s2Out, LOW);
  digitalWrite(piOut, LOW);
  
} // setup

void loop() {
  
  // If only the first sensor detects something
  if (checkSensor(s1Out, s1In) && !checkSensor(s2Out, s2In)) {
    
    long startTime = millis();
    while (millis() - startTime < detectionTime) {
    
      if (checkSensor(s2Out, s2In) && checkSensor(s1Out, s1In)) {
        sendNotification();
        long startTime = millis();
        while (millis() - startTime < 3000) ;  // Pause before starting to check again 
        break;
      } // if - vehicle has entered
      else if (!checkSensor(s1Out, s1In)) {
        break;
      } // else if - likely not a vehicle
    
    } // while - not timed out
    
  } // if - something is in front of the first sensor but not the second

} // loop

/*
 * This method checks if an ultrasonic sensor connected at the given input and output
 * pins detects any object within the preset maximum detection distance. If something
 * is detected it will return true, otherwise it will return false.
 */
boolean checkSensor(int outPin, int inPin) {
  
  // Pulse the trigger pin
  long startTime = micros();
  digitalWrite(outPin, LOW);
  while (micros() - startTime < 2) ; // Set low for 2 microseconds for clean start
  digitalWrite(outPin, HIGH);
  startTime = micros();
  while (micros() - startTime < 10) ; // Set trigger high for 10 microseconds
  digitalWrite(outPin, LOW);
  
  // Time the echo pulse
  long pulse = pulseIn(inPin, HIGH);
  
  // Return true if distance is less than maximum detection distance, false otherwise
  if ((pulse / 58) < distanceLimit) return true;
  else return false;
  
} // checkSensor

/*
 * This method is used to notify the entrance controller that a vehicle is entering the
 * lot. It sets the output pin to the Raspberry Pi HIGH and then waits for the input
 * pin from the Raspberry Pi to be set HIGH to make sure that the Raspberry Pi has
 * received the notification before setting the output pin low again.
 */
void sendNotification() {
  digitalWrite(piOut, HIGH);
  while (!digitalRead(piIn)) ; // Wait for acknowledgement
  digitalWrite(piOut, LOW);
} // sendNotification
