package com.boxeddev;

import com.boxeddev.model.ConnectionInfo;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class JMavlinkSocket {
    public static void main(String[] args) {



        // add your IP address and port
        ConnectionInfo data = new ConnectionInfo("192.168.0.103" , 14550);

        GlobalItem.CONNECTION_TYPE = GlobalItem.ConnectionType.TCP;
        GlobalItem.DATA            = data;

        MavlinkConnectionService service = new MavlinkConnectionService();
        service.StartConnection();


    }
}
