/* 
* Simulation for three parking spaces
* Author: Kaya Abe
*/

// Send data when state changes

const int green = 13;
const int yellow = 12;
const int pushButton = 2;
const int s1 = 3;
const int s2 = 4;
const int s3 = 5;

//int reserved = 0;
long distanceLimit = 20;

//unsigned long time = 0;

void setup()
{
  Serial.begin(9600);

  pinMode(pushButton, INPUT);
  pinMode(green, OUTPUT);
  pinMode(yellow, OUTPUT);
}

void loop()
{
  long sensorAns1 = checkDistance(s1);
  long sensorAns2 = checkDistance(s2);
  long sensorAns3 = checkDistance(s3);
  //readLights();
  // if lightsChanged(), then turnLightsOn()
  turnLightsOn(sensorAns1, sensorAns2, sensorAns3);
  checkReservation();
  delay(1000);
}


// working!
long checkDistance(int pingPin)
{
  long pulse, cm;
  
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);
  
  /*
  // Used for 4-pin sensor
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  */
  
  pinMode(pingPin, INPUT);
//  pulse = pulseIn(echoPin, HIGH); // used for 4-pin sensor
  pulse = pulseIn(pingPin, HIGH);
  cm = distance(pulse);
  //turnLight(cm);
  Serial.print("Sensor ");
  Serial.print(pingPin - 2);
  Serial.print(": ");
  Serial.print(cm);
  Serial.print("cm from obstacle \n");
  //delay(100);
  return cm;
}

long distance(long time)
{
  return time/29/2;
}

void turnLightsOn(long d1, long d2, long d3)
{
  if ((d1 >= distanceLimit) || (d3 >= distanceLimit) || (d3 >= distanceLimit))
  {
    turnOnSemaphore(LOW, HIGH);
  }
  else if ((d1 < distanceLimit) && (d2 < distanceLimit) && (d3 < distanceLimit))
  {
    delay(5000); // in case it is a person or anything else on the space
    if ((d1 < distanceLimit) && (d2 < distanceLimit) && (d3 < distanceLimit))
    {
      turnOnSemaphore(LOW, LOW);
    }
  }
} 

void turnOnSemaphore(int y, int g)
{
  digitalWrite(yellow, y);
  digitalWrite(green, g);
}

// set flag for reservation?
void checkReservation()
{
  if (digitalRead(pushButton) == HIGH)
  {
    //reserved = 1;
    turnOnSemaphore(HIGH, LOW);
    delay(5000);
  }
}
