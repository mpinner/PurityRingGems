import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import hypermedia.net.*; 
import controlP5.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class GemSender extends PApplet {


//download at http://ubaa.net/shared/processing/udp/

//download at www.sojamo.de/libraries/controlp5


/************************************************************************************
 GUI
 ************************************************************************************/

ControlP5 cp5;

DropdownList serialddl;
DropdownList baudddl;
Textlabel arduinoLabel;
Textlabel UDPLabel;
Textlabel incomingPacket;
Button startButton;
Button stopButton;
Textfield ipAddressField;
Textfield incomingPortField;
Textfield outgoingPortField;
Textlabel serialPortName;


public void setupGUI() {
  //the ControlP5 object
  cp5 = new ControlP5(this);

  //start button
  startButton = cp5.addButton("START")
    .setPosition(200, 200)
      .setSize(200, 19)
        ;

  //stop button
  stopButton = cp5.addButton("STOP")
    .setPosition(200, 200)
      .setSize(200, 19)
        ;
  stopButton.hide();

  //Serial Port selector
  serialddl = cp5.addDropdownList("SerialPort")
    .setPosition(50, 100)
      .setSize(200, 200)
        ;
  serialddl.setItemHeight(20);
  serialddl.setBarHeight(15);
  serialddl.captionLabel().set("SELECT ARDUINO SERIAL PORT");
  serialddl.captionLabel().style().marginTop = 3;
  serialddl.captionLabel().style().marginLeft = 3;
  serialddl.valueLabel().style().marginTop = 3;
  //set the serial options
  String SerialList[] = Serial.list(); 
  for (int i=0;i<SerialList.length;i++) {
    String portName = SerialList[i];
    serialddl.addItem(portName, i);
  }
  serialddl.setIndex(1);

  //setup the baud list
  baudddl = cp5.addDropdownList("BaudRate")
    .setPosition(50, 50)
      .setSize(200, 200)
        ;
  baudddl.setItemHeight(20);
  baudddl.setBarHeight(15);
  baudddl.captionLabel().set("SELECT THE BAUD RATE");
  baudddl.captionLabel().style().marginTop = 3;
  baudddl.captionLabel().style().marginLeft = 3;
  baudddl.valueLabel().style().marginTop = 3;
  //the baud options
  for (int i=0;i<serialRateStrings.length;i++) {
    String baudString = serialRateStrings[i];
    baudddl.addItem(baudString, i);
  }
  baudddl.setIndex(4);

  //udp IP/port
  ipAddressField = cp5.addTextfield("IP address")
    .setPosition(300, 30)
      .setAutoClear(false)
        .setText(ipAddress)
          ;
  incomingPortField = cp5.addTextfield("Incoming Port Number")
    .setPosition(300, 80)
      .setAutoClear(false)
        .setText(str(inPort))
          ;

  outgoingPortField = cp5.addTextfield("Outgoing Port Number")
    .setPosition(300, 130)
      .setAutoClear(false)
        .setText(str(outPort))
          ;

  //text labels
  arduinoLabel = cp5.addTextlabel("arduinoLabel")
    .setText("Serial")
      .setPosition(50, 10)
        .setColorValue(0xffffff00)
          .setFont(createFont("SansSerif", 11))
            ;
  UDPLabel = cp5.addTextlabel("UDPLabel")
    .setText("UDP")
      .setPosition(300, 10)
        .setColorValue(0xffffff00)
          .setFont(createFont("SansSerif", 11))
            ;

  incomingPacket = cp5.addTextlabel("incomingPacketLabel")
    .setText("Incoming")
      .setPosition(210, 100)
        .setColorValue(0xffffff00)
          .setFont(createFont("SansSerif", 10))
            ;
  incomingPacket.hide();
  
  serialPortName = cp5.addTextlabel("serialPortNameLabel")
    .setText("Serial Port Name")
      .setPosition(210, 160)
        .setColorValue(0xffffff00)
          .setFont(createFont("SansSerif", 10))
            ;
  serialPortName.hide();
}

public void controlEvent(ControlEvent theEvent) {
  String eventName = theEvent.getName();
  if (theEvent.isGroup()) {
    if (eventName == "SerialPort") {
      //set the serial port 
      serialListNumber = PApplet.parseInt(theEvent.getValue());
    } 
    else if (eventName == "BaudRate") {
      int index = PApplet.parseInt(theEvent.getValue());
      baud = Integer.parseInt(serialRateStrings[index]);
    } 
    else {
    }
  } 
  else if (theEvent.isAssignableFrom(Textfield.class)) {
    if (eventName == "IP address") {
      ipAddressField.setFocus(false);
      ipAddress = theEvent.getStringValue();
    } 
    else if (eventName == "Incoming Port Number") {
      incomingPortField.setFocus(false);
      inPort = Integer.parseInt(theEvent.getStringValue());
    } 
    else if (eventName == "Outgoing Port Number") {
      outgoingPortField.setFocus(false);
      outPort = Integer.parseInt(theEvent.getStringValue());
    }
  }
}

boolean applicationRunning = false;

//start everything
public void START(int theValue) {
  setupUDP();
  setupSerial();
  hideControls();
  applicationRunning = true;
}

//hide all the controls and show the stop button
public void hideControls() {
  serialddl.hide();
  baudddl.hide();
  startButton.hide();
  outgoingPortField.hide();
  incomingPortField.hide();
  ipAddressField.hide();
  incomingPacket.show();
  serialPortName.show();
  arduinoLabel.hide();
  //show the stop button
  stopButton.show();
}

public void showControls() {
  serialddl.show();
  baudddl.show();
  startButton.show();
  outgoingPortField.show();
  incomingPortField.show();
  ipAddressField.show();
  incomingPacket.hide();
  serialPortName.hide();
  arduinoLabel.show();
  //hide the stop button
  stopButton.hide();
}

public void STOP() {
  stopSerial();
  stopUDP();
  showControls();
  applicationRunning = false;
}

/************************************************************************************
 SERIAL
 ************************************************************************************/

//the Serial communcation to the Arduino
Serial serial;

String[] serialRateStrings = {
  "300", "1200", "2400", "4800", "9600", "14400", 
  "19200", "28800", "38400", "57600", "115200"
};
int baud = 9600;
int serialListNumber = 8;

ArrayList<Byte> serialBuffer = new ArrayList<Byte>();

public void setupSerial() {
  serial = new Serial(this, Serial.list()[serialListNumber], baud);
  println(Serial.list()[serialListNumber]);
  serialPortName.setText(Serial.list()[serialListNumber] + "\n"
  + ipAddress + "\n"+ inPort + "\n");
}

public void stopSerial() {
  serial.stop();
}

public void serialEvent(Serial serial) { 
  //decode the message
  while (serial.available () > 0) {
    slipDecode(PApplet.parseByte(serial.read())); 
  }
}

public void SerialSendToUDP() {
  byte [] buffer = new byte[serialBuffer.size()];
  //copy the buffer over
  for (int i = 0; i < serialBuffer.size(); i++) {
    buffer[i] = serialBuffer.get(i);
  }
  //send it off
  UDPSendBuffer(buffer);
  //clear the buffer
  serialBuffer.clear();
  //light up the indicator
  drawIncomingSerial();
}

public void serialSend(byte[] data) {
  //encode the message and send it
  for (int i = 0; i < data.length; i++){
     slipEncode(data[i]);
  }
  //write the eot
  serial.write(eot);
}

/************************************************************************************
 SLIP ENCODING
 ************************************************************************************/

byte eot = PApplet.parseByte(192);
byte slipesc = PApplet.parseByte(219);
byte slipescend = PApplet.parseByte(220);
byte slipescesc = PApplet.parseByte(221);

byte previousByte;

public void slipDecode(byte incoming) {
  byte previous = previousByte;
  previousByte = incoming;
  //if the previous was the escape char
  if (previous == slipesc) {
    //if this one is the esc eot
    if (incoming==slipescend) { 
      serialBuffer.add(eot);
    } 
    else if (incoming==slipescesc) {
      serialBuffer.add(slipesc);
    }
  } 
  else if (incoming==eot) {
    //if it's the eot
    //send off the packet
    SerialSendToUDP();
  } 
  else if (incoming != slipesc) {
    serialBuffer.add(incoming);
  }
}

public void slipEncode(byte incoming) {
  if(incoming == eot){ 
    serial.write(slipesc);
    serial.write(slipescend); 
  } else if(incoming==slipesc) {  
    serial.write(slipesc);
    serial.write(slipescesc); 
  } else {
    serial.write(incoming);
  }  
}


/************************************************************************************
 UDP
 ************************************************************************************/

//UDP communication
UDP udp;

int inPort = 8000;
int outPort = 9001;
String ipAddress = "127.0.0.1";

public void setupUDP() {
  udp = new UDP( this, inPort );
  //udp.log( true );     // <-- printout the connection activity
  udp.listen( true );
}

public void stopUDP() {
  udp.close();
}

public void UDPSendBuffer(byte[] data) {
  udp.send( data, ipAddress, outPort );
}

//called when UDP recieves some data
public void receive( byte[] data) {
  drawIncomingUDP();
  //send it over to serial
  serialSend(data);
}

/************************************************************************************
 SETUP/DRAW
 ************************************************************************************/

public void setup() {
  // configure the screen size and frame rate
  size(550, 300, P3D);
  frameRate(30);
  setupGUI();
  
  //START(-1);
}

public void draw() {
  background(128);
  if (applicationRunning) {
    drawIncomingPackets();
  }
}


/************************************************************************************
 VISUALIZING INCOMING PACKETS
 ************************************************************************************/

int lastSerialPacket = 0;
int lastUDPPacket = 0;

public void drawIncomingPackets() {
  //the serial packet
  fill(0);
//  rect(75, 50, 100, 100);
  //the udp packet
  rect(325, 50, 100, 100);
  int now = millis();
  int lightDuration = 75;
/*  if (now - lastSerialPacket < lightDuration) {
    fill(255);
    rect(85, 60, 80, 80);
  }
*/
  if (now - lastUDPPacket < lightDuration) {
    fill(255);
    rect(335, 60, 80, 80);
  }
}

public void drawIncomingSerial() {
  lastSerialPacket = millis();
}

public void drawIncomingUDP() {
  lastUDPPacket = millis();
}

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "GemSender" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
