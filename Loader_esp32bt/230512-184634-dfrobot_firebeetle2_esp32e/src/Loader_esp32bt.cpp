/* Includes ------------------------------------------------------------------*/
#include "srvr.h" // Server functions
#include <Arduino.h> // Arduino functions
//Modified for FireBeetle 2 ESP32-E

/* Entry point ----------------------------------------------------------------*/
void setup()
{
  // Serial port initialization
  Serial.begin(115200);
  delay(10);

  // Bluetooth initialization
  Srvr__btSetup();

  // SPI initialization
  EPD_initSPI();

  // Initialization is complete
  Serial.print("\r\nOk!\r\n");
}

/* The main loop -------------------------------------------------------------*/
void loop()
{
  Srvr__loop();
}
