/**
  ******************************************************************************
  * @file    srvr.h
  * @author  Waveshare Team
  * @version V2.0.0
  * @date    10-August-2018
  * @brief   ESP8266 WiFi server.
  *          This file provides firmware functions:
  *           + Sending web page of the tool to a client's browser
  *           + Uploading images from client part by part
  *
  ******************************************************************************
  */ 

/* Library includes ----------------------------------------------------------*/
#include "BluetoothSerial.h"
#define Button 23 // Button on GPIO 22


bool Srvr__btIsOn;// It's true when bluetooth is on
bool Srvr__btConn;// It's true when bluetooth has connected client 
int  Srvr__msgPos;// Position in buffer from where data is expected

/* Client ---------------------------------------------------------------------*/
BluetoothSerial SerialBT; // Bluetooth client 
boolean confirmRequestPending = false;

/* Avaialble bytes in a stream ------------------------------------------------*/
int Srvr__available()
{
    return Srvr__btIsOn ? SerialBT.available() : false;
}

void Srvr__write(const char*value)
{
    // Write data to bluetooth
    if (Srvr__btIsOn) SerialBT.write((const uint8_t*)value, strlen(value));
}

int Srvr__read()
{
    return Srvr__btIsOn ? SerialBT.read() : -1;
}

void Srvr__flush()
{
    // Clear Bluetooth's stream
    if (Srvr__btIsOn) SerialBT.flush();  
}

void BTConfirmRequestCallback(uint32_t numVal)
{
  confirmRequestPending = true;
  Serial.println(numVal);
}

void BTAuthCompleteCallback(boolean success)
{
  confirmRequestPending = false;
  if (success)
  {
    Serial.println("Pairing success!!");
  }
  else
  {
    Serial.println("Pairing failed, rejected by user!!");
  }
}

/* Project includes ----------------------------------------------------------*/
#include "buff.h"       // POST request data accumulator
#include "epd.h"        // e-Paper driver

bool Srvr__btSetup()                                              
{
    //Enable SSP
    SerialBT.enableSSP();
    SerialBT.onConfirmRequest(BTConfirmRequestCallback);
    SerialBT.onAuthComplete(BTAuthCompleteCallback);

    // Name shown in bluetooth device list of App part (PC or smartphone)
    String devName("KATAX BADGE");

    // Turning on
    Srvr__btIsOn = SerialBT.begin(devName);

    // Show the connection result
    if (Srvr__btIsOn) Serial.println("Bluetooth is on");
    else Serial.println("Bluetooth is off");

    // There is no connection yet
    Srvr__btConn = false;

    // Return the connection result
    return Srvr__btIsOn;

    //Read button input
    pinMode(Button, INPUT);
}

/* The server state observation loop -------------------------------------------*/
bool Srvr__loop() 
{
    // Wait for confirmation of paired device
      while (confirmRequestPending)
        {
            if(digitalRead(Button) == HIGH){
                SerialBT.confirmReply(true);
            }
            delay(10);
        }

    // Bluetooh connection checking
    if (!Srvr__btIsOn) return false;

    // Show and update the state if it was changed
    if (Srvr__btConn != SerialBT.hasClient())
    {
        Serial.print("Bluetooth status:");
        Serial.println(Srvr__btConn = !Srvr__btConn ? "connected" : "disconnected"); 
    }

    // Exit if there is no bluetooth connection
    if (!Srvr__btConn) return false; 

    // Waiting the client is ready to send data
    while(!SerialBT.available()) delay(1);

    // Set buffer's index to zero
    // It means the buffer is empty initially
    Buff__bufInd = 0;

    // While the stream of 'client' has some data do...
    while (Srvr__available())
    {
        // Read a character from 'client'
        int q = Srvr__read();

        // Save it in the buffer and increment its index
        Buff__bufArr[Buff__bufInd++] = (byte)q;
    }
    //Serial.println();

    // Initialization
    if (Buff__bufArr[0] == 'I')
    {
        // Getting of e-Paper's type
        EPD_dispIndex = Buff__bufArr[1];

        // Print log message: initialization of e-Paper (e-Paper's type)
        Serial.printf("<<<EPD %s", EPD_dispMass[EPD_dispIndex].title);

        // Initialization
        EPD_dispInit();

        Buff__bufInd = 0;
        Srvr__flush();
    }

    // Loading of pixels' data
    else if (Buff__bufArr[0] == 'L')
    {
        // Print log message: image loading
        //Serial.print("<<<LOAD");
        int dataSize = Buff__getWord(1);

        if (Buff__bufInd < dataSize)
        {
            Buff__bufInd = 0;
            Serial.print("ERROR");
            Srvr__write("Error!");
            Srvr__flush();
            return true;
        }
       
        // Load data into the e-Paper 
        // if there is loading function for current channel (black or red)
        if (EPD_dispLoad != 0) EPD_dispLoad();

        Buff__bufInd = 0;
        Srvr__flush();
    }
    
    // Show loaded picture
    else if (Buff__bufArr[0] == (byte)255 && 
    Buff__bufArr[1] == (byte)254 &&
    Buff__bufArr[2] == (byte)253 && 
    Buff__bufArr[3] == (byte)252 
    )
    {
        EPD_dispMass[EPD_dispIndex].show();
                
        Buff__bufInd = 0;
        Srvr__flush();

        //Print log message: show
        //Serial.print("<<<SHOW");
    }

    Srvr__write("Ok!");
    delay(1);

    // Print log message: the end of request processing
    //Serial.print(">>>");
    return true;
}