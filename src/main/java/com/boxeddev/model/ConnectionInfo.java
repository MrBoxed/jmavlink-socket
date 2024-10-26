package com.boxeddev.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionInfo {

    private String ipAddress;
    private int port;

    public ConnectionInfo(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public ConnectionInfo(ConnectionInfo ref) {
        if (ref != null) {
            this.ipAddress = ref.getIpAddress();
            this.port = ref.getPort();
        }
    }

    public ConnectionInfo() {

    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    // function for checking the provided ip string is valid ip address format
    public boolean isValidIPAddress() {

        //if the IP address is empty return false
        if (ipAddress == null) {
            return false;
        }

        // removing all white space from the string
        ipAddress = ipAddress.replaceAll("\\s", "");

        //regex for digit from 0 to 255.
        String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";

        // Regex for a digit from 0 to 255 and followed by a dot, repeat 4 times. this is the regex to validate an IP address.
        String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        //pattern class contains matcher() method to find matching between given IP address and regular expression.
        Matcher m = p.matcher(ipAddress);

        // Return if the IP address matched the ReGex
        return m.matches();
    }

    public boolean isValidPort() {
        return ((port <= 65535) && (port > 0));
    }

    @Override
    public String toString() {
        return ("IP address: " + ipAddress + "\n" + "Port no.:  " + port);
    }
}
