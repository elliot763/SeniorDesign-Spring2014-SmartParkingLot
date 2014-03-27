/* 
* Simulation for one parking space
*/

const int green = 13;
const int yellow = 12;
const int pushButton = 2;
const int trigPin = 3;
const int echoPin = 4;

int reserved = 0;
long distanceLimit = 12;

//unsigned long time = 0;

void setup()
{
  Serial.begin(9600);
  
  pinMode(echoPin, INPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(pushButton, INPUT);
  pinMode(green, OUTPUT);
  pinMode(yellow, OUTPUT);
}

void loop()
{
  long sensorAns = checkDistance();
  turnLightsOn(sensorAns);
  delay(1000);
  checkReservation();
}


// working!
long checkDistance()
{
  long pulse, cm;
  
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  pulse = pulseIn(echoPin, HIGH);
  
  cm = distance(pulse);
  //turnLight(cm);
  Serial.print(cm);
  Serial.print("cm from obstacle \n");
  //delay(100);
  return cm;
}

long distance(long time)
{
  return time/29/2;
}

void turnLightsOn(long distance)
{
  if (distance >= distanceLimit)
  {
    turnOnSemaphore(LOW, HIGH);
  }
  else if (distance < distanceLimit)
  {
    delay(8000); // in case it is a person or anything else on the space
    if (distance < distanceLimit)
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
    delay(4000);
  }
}
