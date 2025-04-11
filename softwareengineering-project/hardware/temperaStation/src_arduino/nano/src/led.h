#if !defined(__led_h)
#define __led_h
#include <Arduino.h>

#define PIN_RED 3
#define PIN_GREEN 5
#define PIN_BLUE 6

static int led_wait_interval = 200; // the time the led stays in on state when its blinking
  
void setLed(int blink, int color);

#endif