#include <Arduino.h>
#include <ArduinoBLE.h>

BLEService deviceInformationService("180A");
// size is defined as "variable/varies"
BLEStringCharacteristic manufacturerNameCharacteristic("2A29", BLERead, 64);

// battery service
BLEService batteryService("180F");
// needs 1 unsigned int byte, so we can use unsigned char characteristic
BLEUnsignedCharCharacteristic batteryLevelCharacteristic("2A19", BLERead | BLENotify);

void blePeripheralConnectHandler(BLEDevice central);
void blePeripheralDisconnectHandler(BLEDevice central);
void readManufacturerName(BLEDevice central, BLECharacteristic characteristic);
void readBatteryLevel(BLEDevice central, BLECharacteristic characteristic);

// var for battery demo
byte batteryLvl = 100;
unsigned long nextBatteryDrainTime = 0;
constexpr unsigned long batteryDrainInterval = 10000;

void setup() {
  Serial.begin(9600);
  while (!Serial);
  Serial.println("Serial started");

  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");

    while(1);
  }
  Serial.println("BLE startet");

  BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);

  BLE.setLocalName("g4t2-Arduino-00000001");
  BLE.setDeviceName("g4t2-Arduino-00000001"); // has tb the same (is expected tb the same)
  BLE.setAdvertisedService(deviceInformationService);

  // characteristic user description
  BLEDescriptor deviceInfoDescriptor("2901", "g4t2-Arduino-00000001"); 
  manufacturerNameCharacteristic.addDescriptor(deviceInfoDescriptor);
  manufacturerNameCharacteristic.writeValue("WS BLE Device Andrea");
  manufacturerNameCharacteristic.setEventHandler(BLERead, readManufacturerName);
  deviceInformationService.addCharacteristic(manufacturerNameCharacteristic);
  BLE.addService(deviceInformationService);

  // battery service
  batteryLevelCharacteristic.setEventHandler(BLERead, readBatteryLevel);
  batteryService.addCharacteristic(batteryLevelCharacteristic);
  BLE.addService(batteryService);  
  batteryLevelCharacteristic.writeValue(batteryLvl);

  nextBatteryDrainTime = millis() + batteryDrainInterval;

  BLE.advertise();
}


void reduceBatteryLevel() {
  // if x ms have passed and battery level > 0, battery lvl is decreased 
  // and new battery lvl is written to batteryLevelCharacteristic
  if (nextBatteryDrainTime <= millis() && batteryLvl > 0) {
    nextBatteryDrainTime += batteryDrainInterval;
    batteryLvl -= 1;
    batteryLevelCharacteristic.writeValue(batteryLvl);
  }
}

void loop() {
  // poll for BLE events; can have optional timeout (default 0 ms)
  BLE.poll();
  reduceBatteryLevel();
  // this is only for demonstration purposes!
}

void blePeripheralConnectHandler(BLEDevice central) {
  Serial.println("Connected event, central: ");
  Serial.println(central.address());
}

void blePeripheralDisconnectHandler(BLEDevice central) {
  Serial.println("Disconnected event, central: ");
  Serial.println(central.address());
}

void readManufacturerName(BLEDevice central, BLECharacteristic characteristic) {
  Serial.println("Characteristic event read: ");
  Serial.println(central.address());
}

void readBatteryLevel(BLEDevice central, BLECharacteristic characteristic) {
  Serial.println("Battery lvl characteristic read: ");
  Serial.println(central.address());
}
