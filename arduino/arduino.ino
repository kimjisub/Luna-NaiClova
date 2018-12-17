#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#include <Adafruit_NeoPixel.h>
#include <WiFiClient.h> 
#include <ESP8266WebServer.h>
#include <ESP8266HTTPClient.h>
#include <Servo.h>
#include "DHT.h"
#ifdef __AVR__
#include <avr/power.h>
#endif
#define NUMPIXELS 8

#define FIREBASE_HOST "url"
#define FIREBASE_AUTH "auth"
#define WIFI_SSID "ssid"
#define WIFI_PASSWORD "passwd"
#define DHTPIN 10
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);
 
Servo myservo;

int pos = 0;
int PIN = 13;
int buzzer = 15;

unsigned long pulse = 0;
float ugm3 = 0;
int GP2Y1023 = 12; 

HTTPClient http;

Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

void setup() {
  Serial.begin(115200);

  pinMode(D1, OUTPUT);
  pinMode(D2, OUTPUT);
  pinMode(GP2Y1023, INPUT);
  pinMode(buzzer, OUTPUT);
  
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("connecting");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println();
  Serial.print("connected: ");
  Serial.println(WiFi.localIP());

  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.stream("/queue");

#if defined (__AVR_ATtiny85__)
  if (F_CPU == 16000000) clock_prescale_set(clock_div_1);
#endif

  pixels.begin();
  myservo.attach(14);
}

int n = 0;

String path;
String data;

void loop() {
  String path = "";
  String data = "";
  if (Firebase.available()) {
    FirebaseObject event = Firebase.readEvent();
    String eventType = event.getString("type");
    eventType.toLowerCase();
    if (eventType == "put") {
      path = event.getString("path");
      data = event.getString("data");
      Serial.print("data: ");
      Serial.println(data);
      Serial.print("data: ");
      Serial.println(path);
    }
  }
  if (data == "light1on") {
    for (int i =0; i < NUMPIXELS; i++){
      pixels.setPixelColor(i, pixels.Color(255, 255, 255));
      pixels.show();
      delay(100);
    }
    removeData(path);
  }
  if (data == "light1off") {
    for (int i =0; i < NUMPIXELS; i++){
      pixels.setPixelColor(i, pixels.Color(0, 0, 0));
      pixels.show();
      delay(100);
    }
    removeData(path); 
  }
  if (data == "light2on") {
    digitalWrite(D2, LOW);
    removeData(path);
  }
  if (data == "light2off") {
    digitalWrite(D2, HIGH);
    removeData(path);
  }
  if (data == "bye") {
    digitalWrite(D1, HIGH);
    digitalWrite(D2, HIGH);
    removeData(path);
  }
  if (data == "dust") {
    pulse = pulseIn(GP2Y1023, LOW, 20000);
    int ugm3 = pulse2ugm3(pulse);
    String dust = String(ugm3);
    String msg = "현재 실내 미세먼지 수치는 ";
    msg += dust;
    msg += "으로 ";
    if (ugm3 < 50) {
      msg += "쾌적한 편입니다.";
    }
    if ((ugm3 >= 50)&&(ugm3 < 100)){
      msg += "양호한 편입니다."; 
    }
    if(ugm3>=100){
      msg += "매우 위험한 수준입니다. 지금 당장 환기하십시요.";
    }
    String postData = "msg=" + msg;

    http.begin("https://us-central1-luna-ai-secretary.cloudfunctions.net/sendResponseMsg/dust");
    http.addHeader("Content-type", "application/x-www-form-urlencoded");

    int httpCode = http.POST(postData);
    String payload = http.getString();
    Serial.println(httpCode);
    Serial.println(payload);
    http.end();
    removeData(path);
  }

  if (data == "environment") {
    int t = dht.readTemperature;
    int h = dht.readHumidity;
    String msg = "현재 실내 온도는 ";
    msg += t;
    msg += "도이고, 실내 습도는 ";
    msg += h;
    msg += "퍼센트입니다."
    String postData = "msg=" + msg;

    http.begin("https://us-central1-luna-ai-secretary.cloudfunctions.net/sendResponseMsg/environment");
    http.addHeader("Content-type", "application/x-www-form-urlencoded");

    int httpCode = http.POST(postData);
    String payload = http.getString();
    Serial.println(httpCode);
    Serial.println(payload);
    http.end();
    removeData(path);
  }

  if (data == "search1") {
    tone(buzzer, 300);
    delay(1000);
    noTone(buzzer);
    removeData(path);
  }

  if (data =="door1open") {
    myservo.write(180);
    removeData(path);
  }

  if (data =="door1close"){
    myservo.write(0);
    removeData(path);
  }
}

void removeData(String path) {
  Firebase.remove("/queue" + path);
}

int pulse2ugm3(unsigned long pulse) {
  float value = (pulse - 1400) / 14.0;
  if (value > 300) {
    value = 0;
  }
  return value;
}
