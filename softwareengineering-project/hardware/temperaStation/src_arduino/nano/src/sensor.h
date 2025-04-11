#if !defined(__senor_h)
#define __senor_h
#include <Arduino.h>
#include <stdio.h>
#include <stdlib.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include "Adafruit_BME680.h"


// define pins for IC connection to BME688
#define BME_SCK 13
#define BME_MISO 12
#define BME_MOSI 11
#define BME_CS 10

// define Pin for the light intensity  sensor (analog input from photoresistor)
#define PIN_LIGHT_INTENSITY A1 

//define Pins for the Buttons to set Work mode
//define button pins
#define  INOFFICE_PIN 7 //G
#define  DEEPWORK_PIN 8
#define  MEETING_PIN 9
#define  OUTOFFOFFICE_PIN 10 // 

//declare enum for workmode
enum Workmode {
  OUT_OF_OFFICE,
  DEEP_WORK,
  MEETING,
  AVAILABLE
};


bool getButton(Workmode *workmode);
//function to get button 
String getWorkmode(Workmode *workmode);
//function to convert Enum in String
class sensor_data {
    private:
    float temperature;
    float humidity;
    int light_intensity;
    float pollution_VOC;


    public:
    sensor_data();
    void set_sensor(float temperature, float humindity, int light_intensity, float  VOC);
    void print();
    float gettemp();
    float gethum();
    float getpollution();
    float getlight();
};


void data_average(sensor_data *data, sensor_data *average, const int length);
int read_light_intensity(void);
Adafruit_BME680 *creatbme(void);

#endif