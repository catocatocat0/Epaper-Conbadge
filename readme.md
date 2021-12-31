Digital EPaper conbadge utilizing a Waveshare 5.65in 7-Color EPaper screen.

The code relies heavily on Waveshare's code (MIT License) and is modified and released under the MIT License.

The 3D models provided are also under the MIT License.

The project utilizes a WROOM 38-pin ESP32,
a MicroUSB charging port PCB,
a 1000mAH Li-Ion Battery,
a MakerFocus 2A 5V Charge Discharge Integrated Module.


Upon connecting to the conbadge via the app, the conbadge must accept the pair request by setting GPIO pin 23 to high.

3mm hex nuts and bolts and 4mm washers are used to secure the acrylic panel to the body.

The waveshare manufacturers only list 7 colors, however, an 8th hidden color can be seen by sending a 0x7 signal to the E-Ink display. This 8th hidden color varies by display and is seen in the commented palette colors in EPaperPicture.java.
