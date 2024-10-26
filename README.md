# jmavlink-socket

## Overview
**jmavlink-socket** is a Java program that establishes a socket connection to communicate with a MAVLink-enabled drone using the DroneFleet library. This allows for real-time telemetry data exchange and command execution with the drone.

- Contains mapping for mavlink modes of **FIXED WING PLANE** & **QUADCOPTER** 

## Features
- Connects to MAVLink-compatible drones using DroneFleet's jmavlink library.
- Receives telemetry data.
- Efficient and stable communication using Java sockets.

## Requirements
- Java 8 or higher
- DroneFleet jmavlink library

## Installation
1. Clone this repository:
``` bash
https://github.com/MrBoxed/jmavlink-socket.git
```
2. Include the **DroneFleet** jmavlink library in your project dependencies.

## Usage
1. Build and run the program:
```
javac JMavlinkSocket.java
java JMavlinkSocket
```
2. The program will establish a socket connection with the drone and start telemetry and command exchange.

## Contributing
Feel free to fork the project and submit pull requests for improvements!