#include "led.h"
#include <Arduino.h>

void setLed(int blink, int color){
    //can set blink to 0=const or 1=blink
    //can adujst the brightness with the first value 0=Off 100=full on
    //mode: 0 = off; 1 = RED; 2 = BLUE; 3 = GREEN;
  static int red = 0;
  static int green = 0;
  static int blue = 0; 
  static bool on = true; 
  static unsigned long onoff = millis();

  //set the color depending on the mode 
  switch(color){
    case 0: 
        red = 0;
        green = 0;
        blue = 0;
        break; //set all colors to 100 
    case 1: 
        red = 255; // set red pin to max volt
        green = 0;
        blue = 0;
        break;
    case 2: 
        blue =255; // set blue pin to max volt
        red = 0;
        green = 0;
        break;
    case 3:
        green = 255; // set green pin to max volt
        red = 0;
        blue = 0;
        break;
    }

    if(blink) {
        if(onoff<millis()) {//if the time is up
        on = !on;           //switch between on and off on the led 
        onoff=millis() + led_wait_interval; //reset the timer
        }
    }

    if(blink == 1 && !on) {            // if led is already on and its supposed to blink turn all leds          
        analogWrite(PIN_RED, 0);      // Arduino PIN 3 Must be connected to RED of RBG
        analogWrite(PIN_GREEN, 0);    // Arduino PIN 5 Must be connected to GREEN of RBG
        analogWrite(PIN_BLUE, 0);     // Arduino PIN 6 Must be connected to BLUE of RBG
        return;
    }
    else{
        analogWrite(PIN_RED, red);      // Arduino PIN 3 Must be connected to RED of RBG
        analogWrite(PIN_GREEN, green);  // Arduino PIN 5 Must be connected to GREEN of RBG
        analogWrite(PIN_BLUE, blue);    // Arduino PIN 6 Must be connected to BLUE of RBG
        on = true;                      // leds are on so set on = true
        return;
    }
}