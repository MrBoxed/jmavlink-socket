package com.boxeddev;
/*
 * ------------------------------------------------------------------------------
 * Project Name: jmavlink-socket
 * Author: MrBoxed
 * GitHub: https://github.com/MrBoxed/jmavlink-socket
 * ------------------------------------------------------------------------------
 * Copyright (c) 2024 MrBoxed
 * This project is licensed under the MIT License.
 * You may use, distribute, and modify this code under the terms of the MIT License.
 * For more details, please see the LICENSE file in this repository.
 * ------------------------------------------------------------------------------
 * Contributions:
 * Contributions to this project are welcome. Feel free to submit issues, fork the
 * repository, or create pull requests. Please follow the contribution guidelines
 * outlined in the CONTRIBUTING.md file.
 * ------------------------------------------------------------------------------
 **/

import com.boxeddev.model.ConnectionInfo;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavResult;
import io.dronefleet.mavlink.common.VfrHud;
import io.dronefleet.mavlink.minimal.Heartbeat;
import io.dronefleet.mavlink.minimal.MavState;
import io.dronefleet.mavlink.minimal.MavType;
import io.dronefleet.mavlink.util.EnumValue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.dronefleet.mavlink.minimal.MavAutopilot.MAV_AUTOPILOT_INVALID;

public class MavlinkConnectionService {

    /// #######################################//
    //  Constant values for checking limit     //
    //  Change as per requirements             //
    /// #######################################//
    private final int HEARTBEAT_LIMIT = 3;  // should be under the timeout limit : checkingLimit < ConnectionTimeout
    private final int TOTAL_ATTEMPTS = 20;
    private final int RECONNECTION_DELAY = 3000;    // in ms
    private int CURRENT_ATTEMPT = 0;


    /// ####################################### ///

    private int heartbeatCounts;            // for counting the incoming heartbeat for knowing connection state
    private boolean tryToConnect;
    public static MavlinkConnection mavlinkConnection; // mavlink connection
    public static MavlinkMessage<?> mavlinkMessage;
    public static AtomicBoolean CONNECTION_LOCK;

    /// ############################################# ///
    ///     Variables for command trigger and result  ///
    /// ############################################# ///

    private EnumValue<MavResult> commandResult;
    private EnumValue<MavCmd> acknowledgeCommand;

    public MavlinkConnectionService() {

        CONNECTION_LOCK = new AtomicBoolean(false);

        heartbeatCounts = (0);
        tryToConnect = true;
        mavlinkConnection = null;
        mavlinkMessage = null;
    }

    /// ########################################################### ///
    ///             CONNECTION FUNCTIONS BELOW THIS                 ///
    /// ########################################################### ///

/**
 * Initiates a connection based on the specified connection type (TCP/UDP),
 * attempting to establish a link within a defined retry limit. If the connection
 * type is null or unrecognized, it logs an error and terminates the attempt.
*/
    public void StartConnection() {

        GlobalItem.ConnectionType type =
                (GlobalItem.CONNECTION_TYPE == null) ? null : GlobalItem.CONNECTION_TYPE;

        if (type == null) {
            System.err.println("Connection type is NULL !!! ");
            return;
        }

        System.out.println("Connection StartMavlinkConnection: " + type.name());

        if (type == GlobalItem.ConnectionType.TCP) {

            // only trying to connect when the attempt is under the limit
            while ((tryToConnect) && (CURRENT_ATTEMPT < TOTAL_ATTEMPTS)) {

                // calling the func to start the connection :)
                TCP_Connection();

            }
            System.out.println("Connecting attempt exceeded :( ");
            GlobalItem.CONNECTION_EXIST = (false);

        } else if (type == GlobalItem.ConnectionType.UDP) {

            // only trying to connect when the attempt is under the limit
            while ((tryToConnect) && (CURRENT_ATTEMPT < TOTAL_ATTEMPTS)) {

                // calling the func to start the connection :)
                UDP_Connection();

            }

                System.out.println("Connecting attempt exceeded :( ");
                GlobalItem.CONNECTION_EXIST = (false);



        } else {
            System.err.println("Unknown Connection Type!!");
        }

    }


    /**
     * Verifies MAVLink connection status by counting Heartbeat messages within a timeout .
     * @return true if the heartbeat count exceeds the limit, indicating a valid connection.
     */
    private boolean CheckConnection() {

        // for connection timeout
        long timeout = System.currentTimeMillis() + GlobalItem.TIMEOUT;
        heartbeatCounts = 0;
        try {

            while (System.currentTimeMillis() < timeout) {

                // Check if timeout has been exceeded
                if (((mavlinkMessage = mavlinkConnection.next()) != null)) {

                    // Process the message payload if it's a Heartbeat
                    if (mavlinkMessage.getPayload() instanceof Heartbeat) {
                        System.out.println("GOT Heartbeat for connection check");
                        heartbeatCounts++;
                    }
                }
            }

            GlobalItem.CONNECTION_EXIST = (heartbeatCounts > HEARTBEAT_LIMIT);
            CURRENT_ATTEMPT = (heartbeatCounts > HEARTBEAT_LIMIT) ? 0 : CURRENT_ATTEMPT ;

            System.out.println("Exit from the Checking Loop :)");

        } catch (IOException exception) {
            System.err.println("CheckMavlinkConnection() failed: " + exception);
        }

        return (heartbeatCounts >= HEARTBEAT_LIMIT);
    }


    /**
     * Manages the MAVLink connection loop, checking for a valid connection and processing incoming messages.
     * If the connection is lost or null, it sets the connection status to false.
     * @throws IOException
     */
    private void ConnectionLoop() throws IOException {

        if (mavlinkConnection != null) {

            if (!CheckConnection()) {
                System.err.println("Connection does not exist :( ");
                return;
            }

            if (tryToConnect) {

                while (GlobalItem.CONNECTION_EXIST) {

                    if (!CONNECTION_LOCK.get()) {

                        mavlinkMessage = mavlinkConnection.next();

                        // Check for the incoming messages
                        ForIdentifyMavlinkMessage(mavlinkMessage);

                    }

                }
            }

        }


        else {
            System.out.println("Connection mavlinkConnection = NULL");
            GlobalItem.CONNECTION_EXIST = (false);
        }

        GlobalItem.CONNECTION_EXIST = (false);
    }


    /**
     * Attempts to establish a TCP connection for MAVLink communication.
     * If the connection fails after the maximum attempts, it sets the connection status to false.
     * Successful connections lead to entering the connection loop for message processing.
     */
    private void TCP_Connection() {

        if (CURRENT_ATTEMPT > TOTAL_ATTEMPTS) {
            tryToConnect = false;
            return;
        }

        CURRENT_ATTEMPT++;

        ConnectionInfo tcpDataRef = (GlobalItem.DATA == null) ? null : GlobalItem.DATA;

        if (tcpDataRef == null) {
            System.out.println("Connection TCP_DATA_REF is NULL = returned");
            return;
        }

        try {

            // Setting the address & socket for connection :)
            SocketAddress address = new InetSocketAddress(tcpDataRef.getIpAddress(), tcpDataRef.getPort());
            Socket socket = new Socket();

            // if it does not connect within time limit it will throw error
            socket.connect(address, RECONNECTION_DELAY);

            System.out.println("Socket connected Successfully");

            // if socket connection was able to connect with the port,
            if (socket.isConnected()) {

                mavlinkConnection = MavlinkConnection.create(socket.getInputStream(), socket.getOutputStream());

                ConnectionLoop();

            } else {

                GlobalItem.CONNECTION_EXIST = (false);
                System.err.println("CONNECTION SOCKET FAILED TO CONNECT WITH MAVLINK");

                socket.close();

                return;
            }

            socket.close();

        } catch (IOException e) {
            System.err.println("Connection socket timeout: tcp mavlink service " + e.getMessage());
        }


    }


    /**
     * Attempts to establish a UDP connection for MAVLink communication.
     * It sets up a datagram socket, handles incoming packets, and initializes the MAVLink connection.
     * If the connection's attempts exceed the limit, it stops further attempts and logs any errors during the process.
     */
    private void UDP_Connection() {

        if (CURRENT_ATTEMPT > TOTAL_ATTEMPTS) {
            tryToConnect = false;
            return;
        }

        CURRENT_ATTEMPT++;

        ConnectionInfo udpDataRef = (GlobalItem.DATA == null) ? null : GlobalItem.DATA;
        if (udpDataRef == null) return;

        // if mavlink connection have not been created already then create new and connection
        if (mavlinkConnection == null) {
            //Log.d("CONNECTION", "IN THREAD");

            // The address we listen on to receive/send packets
            SocketAddress remoteAddress = new InetSocketAddress(udpDataRef.getIpAddress(), udpDataRef.getPort());

            // The size we use for buffers. 65535 should be able to carry any
            // UDP packet of a standard length.
            int bufferSize = 65535;

            try {
                // This binds to the listen address, we can later use this datagram socket
                // to receive packets and mark packets with source addresses when sending.
                DatagramSocket udpSocket = new DatagramSocket();
                udpSocket.connect(remoteAddress);

                //udpSocket.connect( address );

                PipedInputStream udpIn = new PipedInputStream();

                OutputStream udpOut = new OutputStream() {

                    final byte[] buffer = new byte[bufferSize];
                    int position = 0;

                    @Override
                    public void write(int b) throws IOException {

                        write(new byte[]{(byte) b}, 0, 1);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {

                        // if the buffer is full, we flush
                        if ((position + len) > (buffer.length)) {
                            flush();
                        }
                        System.arraycopy(b, off, buffer, position, len);
                        position += len;
                    }

                    @Override
                    public void flush() throws IOException {
                        DatagramPacket packet = new DatagramPacket(buffer, 0, position, remoteAddress);
                        udpSocket.send(packet);
                        position = 0;
                    }
                };

                // We connect `udpIn` to a corresponding PipedOutputStream (`appOut`). Data that we write to
                // `appOut` will be available to read from `udpIn`.
                // We instantiate/connect here rather than within the reading thread so that `udpIn` can be used immediately.
                PipedOutputStream appOut = new PipedOutputStream(udpIn);


                ExecutorService service = Executors.newSingleThreadExecutor();
                service.execute(() -> {
                    try {
                        DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
                        while (!udpSocket.isClosed()) {
                            udpSocket.receive(packet);
                            appOut.write(packet.getData(), packet.getOffset(), packet.getLength());
                            appOut.flush();
                        }
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    } finally {
                        try {
                            appOut.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!udpSocket.isClosed()) {
                            udpSocket.close();
                        }
                        if (!service.isShutdown()) {
                            service.shutdown();
                        }
                    }
                });

                // if udp connection was able to connect with the port,
                // then set the state of the connection to true
                if (udpSocket.isConnected()) {

                    Heartbeat heartbeat = Heartbeat.builder()
                            .type(MavType.MAV_TYPE_GCS)
                            .autopilot(MAV_AUTOPILOT_INVALID)
                            .systemStatus(MavState.MAV_STATE_UNINIT)
                            .mavlinkVersion(3)
                            .build();
                    mavlinkConnection = MavlinkConnection.create(udpIn, udpOut);
                    mavlinkConnection.send1(GlobalItem.SYSTEM_ID, GlobalItem.COMPONENT_ID, heartbeat);
                    ConnectionLoop();

                } else {
                    udpSocket.close();
                    System.out.println("UDP SOCKET FAILED TO CONNECT");
                    return;
                }

                udpSocket.close();

            } catch (IOException e) {

                System.err.println("UDP socket timeout: " + e.getMessage());

            }
        }
    }

    /**
     * Checking for the incoming message from mavlinkConnection
     * @param message {@link MavlinkMessage}
     */
    private void ForIdentifyMavlinkMessage(MavlinkMessage<?> message) {

        // Heartbeat (# 0)
        if (message.getPayload() instanceof Heartbeat heartbeat) {
            System.out.println("::::HeartBeat::::");
        }

        // VFR_HUD ( #74 ) for air speed and ground speed
        else if (message.getPayload() instanceof VfrHud hud) {
            System.out.println("AirSpeed    :" + hud.airspeed());
            System.out.println("GroundSpeed :" + hud.groundspeed());
            System.out.println("Heading     :" + hud.heading());
            System.out.println("Throttle    :" + hud.throttle());
            System.out.println("Altitude    :" + hud.alt());
            System.out.println("Climb       :" + hud.climb());
        }

    }

    private void DisconnectConnection() {
        switch (GlobalItem.CONNECTION_TYPE) {
            case TCP: // TODO: make connection release ;
            case UDP: // TODO: make connection release ;
            case USB: // TODO: make connection release ;

        }
    }


    private static void SendMavlinkCommands(int systemId, int componentId, Object command) throws IOException {

        if (mavlinkConnection != null && GlobalItem.CONNECTION_EXIST)
            mavlinkConnection.send1(systemId, componentId, command);

    }
}
