package com.boxeddev;

import com.boxeddev.model.ConnectionInfo;
import io.dronefleet.mavlink.minimal.MavType;

public class GlobalItem {

    /// ################################### ///
    ///     FOR DEFINING THE TYPE OF        ///
    ///    CONNECTION TRYING TO ESTABLISH   ///
    /// ################################### ///
    public enum ConnectionType {
        TCP,
        USB,
        UDP
    }

    /// ################################### ///
    ///   MAPPING FOR THE FIXED WING PLANE  ///
    /// ################################### ///
    public enum MODE_MAPPING_APM {
        MANUAL, CIRCLE, STABILIZE, TRAINING, ACRO, FBWA, FBWB, CRUISE, AUTOTUNE, NONE, AUTO, RTL, LOITER, TAKEOFF,
        AVOID_ADSB, GUIDED, INITIALISING, QSTABILIZE, QHOVER, QLOITER, QLAND, QRTL, QAUTOTUNE, QACRO, THERMAL, LOITERALTQLAND
    }

    /// ################################### ///
    ///   MAPPING FOR THE QUADROTOR///
    /// ################################### ///
    public enum MODE_MAPPING_ACM {
        STABILIZE, ACRO, ALT_HOLD, AUTO, GUIDED, LOITER, RTL, CIRCLE, POSITION, LAND, OF_LOITER, DRIFT, SPORT, FLIP, AUTOTUNE,
        POSHOLD, BRAKE, THROW, AVOID_ADSB, GUIDED_NOGPS, SMART_RTL, FLOWHOLD, FOLLOW, ZIGZAG, SYSTEMID, AUTOROTATE, AUTO_RTL,
    }


    public static ConnectionType CONNECTION_TYPE;

    /// ############################# ///
    ///     VARIABLES FOR MAVLINK     ///
    /// ############################# ///
    public static int SYSTEM_ID     = 255; // system id of ground station
    public static int COMPONENT_ID  = 1; // component id
    public static int TIMEOUT       = 5000;

    public static boolean IS_ARM;
    public static boolean IS_GUIDED;
    public static boolean CONNECTION_EXIST;
    public static MavType DRONE_TYPE;
    public static ConnectionInfo DATA;

    public GlobalItem() {

        DATA            = null;
        CONNECTION_TYPE = null;
        DRONE_TYPE      = null;

        IS_ARM = false;
        IS_GUIDED = false;
        CONNECTION_EXIST = false;

    }


}

