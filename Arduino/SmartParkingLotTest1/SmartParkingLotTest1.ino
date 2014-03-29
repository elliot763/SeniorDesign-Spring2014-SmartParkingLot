const int green = 0;
const int yellow = 1;
const int red = 2;
const int trigPin = 3;
const int echoPin = 4;

int reserved = 0;
int distanceLimit = 60;

void setup()
{
  Serial.begin(9600);
  Serial.flush();
  
  pinMode(echoPin, INPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(green, OUTPUT);
  pinMode(red, OUTPUT);
  pinMode(yellow, OUTPUT);
}

void loop()
{
  long pulse, cm;
  
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  
  pulse = pulseIn(echoPin, HIGH);
  
  cm = distance(pulse);
  turnLight(cm);
  Serial.print(cm);
  Serial.print("cm from obstacle \n");
  delay(100);
}

long distance(long time)
{
  return time/29/2;
}

void turnLight(long distance)
{
  if (distance > distanceLimit)
  {
    turnOnSemaphore(0, 0, 1);
  }
  else if (distance < distanceLimit)
  {
    turnOnSemaphore(1, 0, 0);
  }
  else if (reserved)
  {
    turnOnSemaphore(0, 1, 0);
  }
} 

void turnOnSemaphore(int r, int y, int g)
{
  digitalWrite(red, r);
  digitalWrite(yellow, y);
  digitalWrite(green, g);
}
