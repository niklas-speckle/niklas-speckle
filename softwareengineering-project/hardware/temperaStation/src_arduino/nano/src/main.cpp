#include <Arduino.h>
#include <Adafruit_BME680.h>
#include <Stream.h>
#include "led.h"
#include "sensor.h"
#include <ArduinoBLE.h>

char id[] = "G4T2-TD-1";
//setup for BLE 

// services
BLEService sensorService("0x181C");               //0x181C User Data Service 
BLEService workmodeService("0x1811");             //0x1811 Alert Notification Service

//Define all Characteristics  as String 
BLEStringCharacteristic deviceInfoCharacteristic("0x2A00", BLERead, 64);        // Manufacturer Name String
BLEStringCharacteristic temperaturCharacteristic("0x2A6E", BLERead, 64);        // Temperature Characteristics
BLEStringCharacteristic humidityCharacteristic("0x0544", BLERead, 64);          // Humidity Sensor Characteristics
BLEStringCharacteristic air_qualityCharacteristic("0x0542", BLERead, 64);       // Air quality Sensor Characteristics
BLEStringCharacteristic light_intensityCharactersitic("0x054B",BLERead, 64);    // Ambient Light Sensor Characteristics
BLEByteCharacteristic dataGotReadCharactersitic("2A7D", BLERead | BLEWrite );    // Descriptor Value Changed Characteristics
BLEStringCharacteristic workmodeCharacteristic("0x2BDE", BLERead , 16);         // Fixed String 64
BLEByteCharacteristic workmodeGotReadCharactersitic("0x2A7D", BLERead | BLEWrite );    // Descriptor Value Changed Characteristics

// declare function for connectHandler
void blePeripheralConnectHandler(BLEDevice central);
void blePeripheralDisconnectHandler(BLEDevice central);
void readHandler(BLEDevice central, BLECharacteristic characteristic);
void buttonHandler(BLEDevice central, BLECharacteristic characteristic);
void changeHandler(BLEDevice central, BLECharacteristic characteristic);

//enum to store the workmode
Workmode workmode = OUT_OF_OFFICE; 
static bool buttonchanged = false; 

//declare everything for messurement and logic variables for Workmode 
const int num_messurements = 6; //number of messuremnts which get averaged 
sensor_data data[num_messurements]; 
sensor_data average_data; //to store the current average from the last messurement intervall
static int counter = 0; //counter for the array index
Adafruit_BME680 *bme; // I2C
unsigned long messurement_time = 0;         //stores the time when the sensor data got messured 
unsigned long messurement_time_button = 0;  //stores the time when a button got pressed  
unsigned long messurement_interval_sensor = 10000; //10000 messure every 10 seconds
unsigned long messurement_interval_button = 1000; //10000 messure every 10 seconds
unsigned long led_interval = 100;       //time in ms the LED is turned off, when new Workmode gets set
unsigned long sensor_next_read_time = 0;    //since millis starts with 0 when TD is turned on we want to read from that moment on 
unsigned long read__interval = 90000;      //max time interval for reading data (5min)


void setup() {
  //initalize serial connection with baudrate 9600;
  Serial.begin(9600);

  //SET Button PINS to INPUT PULLUP
  pinMode(INOFFICE_PIN, INPUT_PULLUP);
  pinMode(DEEPWORK_PIN, INPUT_PULLUP);
  pinMode(MEETING_PIN, INPUT_PULLUP);
  pinMode(OUTOFFOFFICE_PIN, INPUT_PULLUP);

  //initalize bme
  bme = creatbme();
  // initalize the BLE 
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    while(1);
  }
  Serial.println("BLE startet");

  BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);

  BLE.setLocalName(id);
  BLE.setDeviceName(id); // not needed we only compare local name, so less data is transmittet if not set
  
  // characteristic user description
  BLEDescriptor deviceInfoDescriptor("0x2A00", "Device Name"); 
  BLEDescriptor temperaturDescriptor("0x2A6E", "temperature"); 
  BLEDescriptor humidityDescriptor("0x0544", "humidity"); 
  BLEDescriptor light_intensityDescriptor("0x054B", "light_intensity"); 
  BLEDescriptor air_qualityDescriptor("0x0542", "air_quality"); 
  BLEDescriptor workmodeDescriptor("0x2BDE", "workmode");
  BLEDescriptor readDataDescriptor("0x2A7D", "readDataDescriptor" );    
  BLEDescriptor dataGotReadDescriptor("0x2A7D", "dataGotReadVariable" );    
  
  //deviceInfo characteristic
  deviceInfoCharacteristic.setEventHandler(BLERead, readHandler);
  deviceInfoCharacteristic.addDescriptor(deviceInfoDescriptor);
  sensorService.addCharacteristic(deviceInfoCharacteristic);
  //temperatur characteristic 
  temperaturCharacteristic.setEventHandler(BLERead, readHandler);
  temperaturCharacteristic.addDescriptor(temperaturDescriptor);
  sensorService.addCharacteristic(temperaturCharacteristic);
  //humidityCharacteristic
  humidityCharacteristic.setEventHandler(BLERead, readHandler);
  humidityCharacteristic.addDescriptor(humidityDescriptor);
  sensorService.addCharacteristic(humidityCharacteristic);
  //air_qualityCharacteristic
  air_qualityCharacteristic.setEventHandler(BLERead, readHandler);
  air_qualityCharacteristic.addDescriptor(air_qualityDescriptor);
  sensorService.addCharacteristic(air_qualityCharacteristic);
  //light_intensityCharactersitic
  light_intensityCharactersitic.setEventHandler(BLERead, readHandler);
  light_intensityCharactersitic.addDescriptor(light_intensityDescriptor);
  sensorService.addCharacteristic(light_intensityCharactersitic);
  //GotReadVariableCharacteristic
  dataGotReadCharactersitic.setEventHandler(BLERead, changeHandler);
  dataGotReadCharactersitic.addDescriptor(dataGotReadDescriptor);
  sensorService.addCharacteristic(dataGotReadCharactersitic);
  
  // workmode service 
  workmodeCharacteristic.setEventHandler(BLERead, buttonHandler);
  workmodeCharacteristic.addDescriptor(workmodeDescriptor);

  workmodeGotReadCharactersitic.setEventHandler(BLERead, changeHandler);
  workmodeGotReadCharactersitic.addDescriptor(readDataDescriptor);
  
  workmodeService.addCharacteristic(workmodeCharacteristic);
  workmodeService.addCharacteristic(workmodeGotReadCharactersitic);
  
  BLE.addService(sensorService);
  BLE.addService(workmodeService);

  //set inital values for characteristic
  temperaturCharacteristic.writeValue("None"); // set  initial value of temperature characteristic
  humidityCharacteristic.writeValue("None"); // set  initial value of humidity characteristic
  air_qualityCharacteristic.writeValue("None"); // set  initial value of air_quality characteristic
  light_intensityCharactersitic.writeValue("None"); // set  initial value of light_intensity characteristic
  workmodeCharacteristic.writeValue(getWorkmode(&workmode)); // initalize workmode
  workmodeGotReadCharactersitic.writeValue(buttonchanged);
  deviceInfoCharacteristic.writeValue(id);
  
  BLE.advertise();
}

void loop() {
  // poll for BLE events; can have optional timeout (default 0 ms)
  BLE.poll();  // Handle BLE events

  if(buttonchanged  && workmodeGotReadCharactersitic.value() ){
    buttonchanged = 0;
  }
  
  if(dataGotReadCharactersitic.value()){
    sensor_next_read_time = millis() + read__interval ; 
    dataGotReadCharactersitic.writeValue(0);
  }
  //if the last read time is longer ago than read_interval let led blink red to indikate error: 
  //set workmode got read to false as well, so when its reconnecting it will read the value again
  //otherwise show the current workmode
  
  if(millis() > sensor_next_read_time){
    setLed(1,1);
    //workmodeGotReadCharactersitic.writeValue(1);
  }
  else if(messurement_time_button + led_wait_interval > millis() ){
    setLed(0,0);
  }
  else{             
    setLed(0,workmode);
  }
    
  if(messurement_time_button + messurement_interval_button  < millis()){
  //check buttons, if a button got pressed (doesn't need to be a different one) do a status update per ble (change buttonChanged to true)
  //check the messurmeent time, can only change the workmode in the messurement time interval, 
  // this reduces reading error and multisetting a mode (button not a switch)

    if(getButton(&workmode)){

      buttonchanged = true; 

      //set the charateristic 
      workmodeGotReadCharactersitic.writeValue(0);
      workmodeCharacteristic.writeValue(getWorkmode(&workmode));

      //set new time when button can be changed again
      messurement_time_button = millis(); 
      

      Serial.print("The Workdmode is:");
      Serial.println(getWorkmode(&workmode));
    }
  }

  // take a messurement every given interval
  if(millis() > messurement_time ){
    
    //read data from sensor
    bme->beginReading();
    if (!bme->endReading()) {
      Serial.println(F("Failed to complete reading :("));
      return;
    }
    else{
     //store data from BME Objekt  in array and stroe messurement_time
      data[counter].set_sensor(bme->temperature,bme->humidity,read_light_intensity(),bme->gas_resistance / 1000.0);
     //update when the messurement happend and add the messurement interval to it 
      messurement_time = millis() + messurement_interval_sensor;
      
      //increase the counter if the last index of the messurement array is not reached 
      if(counter<num_messurements-1){
        counter++;
      }
      //if the array of messurements reached the last index calculate the 
      //average data and reset the counter to the beginning of the array
      else{
        counter=0;
        data_average(data,&average_data,num_messurements);
        
        // write the averaged data to the characteristics
        temperaturCharacteristic.writeValue(String(average_data.gettemp()));
        humidityCharacteristic.writeValue(String(average_data.gethum()));
        air_qualityCharacteristic.writeValue(String(average_data.getpollution()));
        light_intensityCharactersitic.writeValue(String(average_data.getlight()));
      }
    }
  }
  
}


//when implementation is completed, the function does not need to do anything and will be changed to do nothing
void blePeripheralConnectHandler(BLEDevice central) {
  Serial.println("Connected event, central: ");
  Serial.println(central.address());
}

void blePeripheralDisconnectHandler(BLEDevice central) {
  Serial.println("Disconnected event, central: ");
  Serial.println(central.address());
}

void readHandler(BLEDevice central, BLECharacteristic characteristic){
  //Serial.println("Read Handler got called");
}

void buttonHandler(BLEDevice central, BLECharacteristic characteristic){
  //Serial.print("workmodeGotReadCharactersitic status: ");
  //Serial.print(workmodeGotReadCharactersitic.value());
  //Serial.println(" got read");
}
void changeHandler(BLEDevice central, BLECharacteristic characteristic){
  // Serial.println("Button changed got read");
     
}
