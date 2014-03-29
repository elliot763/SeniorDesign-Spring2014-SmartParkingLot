/* 
* Simulation for entrance sensors
*/

const int light1 = 13;
const int light2 = 12;
const int pingPin1 = 3;
const int pingPin2 = 4;

const int distanceLimit = 10;
//unsigned long time = 0;

void setup()
{
  Serial.begin(9600);
  
  pinMode(light1, OUTPUT);
  pinMode(light2, OUTPUT);
}

void loop()
{
  long sensorAns1 = checkDistance(pingPin1);
  long sensorAns2 = checkDistance(pingPin2);
  blink(sensorAns1, sensorAns2);
  delay(1000);
}

void blink(long s1, long s2)
{
   if(s1 <= distanceLimit)
  {
    digitalWrite(light1, HIGH);
    digitalWrite(light2, LOW);
    
    if(s2 <= distanceLimit)
    {
      digitalWrite(light1, LOW);
      digitalWrite(light2, HIGH);
      delay(3000);
    }
  }
  else
  {
    digitalWrite(light1, LOW);
    digitalWrite(light2, LOW);
  }
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
  Serial.print(pingPin);
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
