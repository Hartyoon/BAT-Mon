package com.example.bat_mon.BackEnd;

import android.util.Log;

import com.example.bat_mon.BackEnd.BatMonError.ErrorCode;
import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BackEnd.FaultRegisterLookup.RegisterNames;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class FaultRegisterManager {

    public static final int registerCount = 9;
    public static long secondsToError = 3; // How long the FR manager will tolerate a set bit until it generates an error

    private final int segment, cid;

    // When updated the new fault registers are compared against the ones from last time. This is the temporary storage
    public int[] faultRegisters = new int[registerCount];

    // When a FR state (bit) is set to 1, the time of that event is cached.
    // If the bit is not set to 0 again before the threshold is passed, a error will be generated.
    private final LocalDateTime[][] faultRegisterTimers = new LocalDateTime[registerCount][16];
    private long[] balancingTimes = new long[11];
    private final LocalDateTime[] faultRegisterBalancingTimersStart = new LocalDateTime[16];

    private boolean registerPanic = false;

    public long[] getBalancingTimes() {
        return balancingTimes;
    }
    public FaultRegisterManager(int segment, int cid) {
        this.segment = segment;
        this.cid = cid;
    }

    public void updateFaultRegister(int[] newFaultStates) {
        registerPanic = false;
        //update the balancing times before updating the faultStates
        balancingTimes = balancingTime(intToBooleanArray(newFaultStates[2]));


        for (int i = 0; i < registerCount; i++) {
            if (newFaultStates[i] != faultRegisters[i]) {
                Log.i("Fault-Registers", "FR " + segment + " " + cid + " " + RegisterNames.values()[i] + " changed: " +
                    Integer.toBinaryString(faultRegisters[i]) + "\t-->\t" + Integer.toBinaryString(newFaultStates[i]));
            }
            if (checkFaultRegister(i, newFaultStates[i]))   // Update timers and generate error message when necessary
                registerPanic = true; // If any of the registers has a fatal error, the registerPanic flag will be set
            faultRegisters[i] = newFaultStates[i];      // Store current states
        }
    }

    private boolean checkFaultRegister(int regNr, int register) {
        boolean[] newStates = intToBooleanArray(register);
        boolean[] oldStates = intToBooleanArray(faultRegisters[regNr]);
        RegisterNames regName = RegisterNames.values()[regNr];
        String[][] regLookup = FaultRegisterLookup.getRegister(regName);

        boolean fatalError = false;

        for (int i = 0; i < newStates.length; i++) {
            // When a bit changes, update the time stamp. When it's 1 update to current time. When it's 0 set the time stamp to null
            if (newStates[i] != oldStates[i])
                faultRegisterTimers[regNr][i] = newStates[i] ? LocalDateTime.now() : null;

            // When the time stamp is null, the bit is either 0 or the error message was already generated. No need to continue
            if (faultRegisterTimers[regNr][i] == null)
                continue;

            // Calculate passed time since bit was first set to 1. When threshold is passed, generate a new error / set panic mode
            long timeSpan = ChronoUnit.SECONDS.between(faultRegisterTimers[regNr][i], LocalDateTime.now());
            if (timeSpan >= secondsToError) {
                // We only want to generate an error the first time a bit is set to 1.
                // After the error message was generated, we will set the time stamp to null again
                faultRegisterTimers[regNr][i] = null;

                // Generate error message
                Priority prio = Priority.valueOf(regLookup[i][2]);
                if (prio == Priority.NONE)// We ignore NONE priorities
                    continue;
                String message = "Seg " + segment + " CID " + cid + " " + regName + ": " + regLookup[i][0] + ": " + regLookup[i][1];
                BatMonError error = new BatMonError(message, prio, ErrorCode.FAULT_REGISTER_FAULT.getType(), ErrorCode.FAULT_REGISTER_FAULT);
                ErrorHandler.addError(error);

                if (prio == Priority.FATAL)
                    fatalError = true;
            }
        }

        return fatalError;
    }

    private static boolean[] intToBooleanArray(int register) {
        boolean[] state = new boolean[16];
        for (int i = 0; i < state.length; i++)
            state[i] = ((register >> i) & 1) == 1; // Get bit at pos i and convert it to boolean
        return state;
    }

    public boolean isBalancing(int cellID){
        return intToBooleanArray(faultRegisters[2])[cellID];
    }

    //return or saves the time a cell balanced in its latest cycle(and adds it to the total time for statistics) not final yet
    public long[] balancingTime(boolean[] newState){
        long[] newBalancingTimes = new long[11];

        for(int i = 0; i<11 ; i++){
            boolean oldState = isBalancing(i); //the old/currentState of the cell bevor the update

            if(!newState[i] && oldState){ //Cell started balancing
                faultRegisterBalancingTimersStart[i] = LocalDateTime.now();
                Log.i("FaultBTimers", "Start Time of Cell " + i + ": " + faultRegisterBalancingTimersStart[i].toString());
            } else if (newState[i] && !oldState && faultRegisterBalancingTimersStart[i] != null) {//Cell ended balancing
                Log.i("FaultBTimers", "End Time of Cell " + i + ": " + LocalDateTime.now());
                newBalancingTimes[i] =  ChronoUnit.SECONDS.between(faultRegisterBalancingTimersStart[i], LocalDateTime.now());
            }
        }
        return newBalancingTimes; //if the balancing hasn't finished no time should be return, only if the cell finished balancing
    }

    public boolean isRegisterPanic() {
        return registerPanic;
    }
}

/**
 * Class to look up names and description of fault states. A fault always has a name, a description and a priority
 * Make sure to run the FaultRegisterManagerTest.faultRegisterLookup_isCorrect() test if you edit this!
 * Otherwise you might run into runtime errors!
 * Fault with the priority NONE will be ignored and will not generate an error message!
 */
class FaultRegisterLookup {

    public enum RegisterNames {
        FAULT1_STATUS,
        FAULT2_STATUS,
        FAULT3_STATUS,
        COM_STATUS,
        AN_OT_UT_FLT,
        CELL_UV_FLT,
        CELL_OV_FLT,
        CB_OPEN_FLT,
        CB_SHORT_FLT
    };

    public static String[][] getRegister(RegisterNames register) {
        switch (register) {
            case FAULT1_STATUS: return register1;
            case FAULT2_STATUS: return register2;
            case FAULT3_STATUS: return generateRegister("EOT_CB", "", " balancing based on timer done", "INFO");
            case COM_STATUS: return getRegister4(); // TODO: Register 4 is a counter
            case AN_OT_UT_FLT: return getRegister5("FATAL");
            case CELL_UV_FLT: return generateRegister("CT", "_UV_FLT", " under-voltage detected", "NONE");
            case CELL_OV_FLT: return generateRegister("CT", "_OV_FLT", " over-voltage detected", "NONE");
            case CB_OPEN_FLT: return generateRegister("CB", "_OPEN_FLT", " open load detected", "NONE");
            case CB_SHORT_FLT: return generateRegister("CB", "_SHORT_FLT", " short circuit detected", "NONE");
        }
        return null;
    }

    private static final String[][] register1 = {
            { "CT_UV_FLT", "Under-voltage was detected", "FATAL" },
            { "CT_OV_FLT", "Over-voltage was detected", "FATAL" },
            { "AN_UT_FLT", "Under-temperature was detected", "FATAL" },
            { "AN_OT_FLT", "Over-temperature was detected", "FATAL" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "COM_ERR_FLT", "A Message with errors was received", "WARNING" },
            { "COM_LOSS_FLT", "The connection dropped out at some point", "WARNING" },
            { "VPWR_LV_FLT", "IC supply voltage is low", "ERROR" },
            { "VPWR_OV_FLT", "OC supply voltage was too high", "ERROR" },
            { "COM_ERR_OVR_FLT", "Communication error counter overflowed", "INFO" },
            { "RESET_FLT", "Device has been reset", "INFO" },
            { "POR", "Power on reset happened", "INFO" }
    };

    private static final String[][] register2 = {
            { "FUSE_ERR_FLT", "IC malfunction (could not load fuses)", "FATAL" },
            { "DED_ERR_FLT", "IC malfunction (ECC error)", "FATAL" },
            { "OSC_ERR_FLT", "IC malfunction (oscillator error)", "FATAL" },
            { "CB_OPEN_FLT", "Open load was detected on a cell balancer", "FATAL" },
            { "CB_SHORT_FLT", "Short circuit was detected on a cell balancer", "FATAL" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "", "", "NONE" },
            { "IC_TSD_FLT", "IC is overheating", "FATAL" },
            { "GND_LOSS_FLT", "IC malfunction (a GND potential was lost)", "FATAL" },
            { "ADC1_A_FLT", "IC malfunction (ADC-A is broken)", "FATAL" },
            { "ADC1_B_FLT", "IC malfunction (ADC-B is broken)", "FATAL" },
            { "VANA_UV_FLT", "Analog supply under-voltage", "FATAL" },
            { "VANA_OV_FLT", "Analog supply over-voltage", "FATAL" },
            { "VCOM_UV_FLT", "Communication supply under-voltage", "FATAL" },
            { "VCOM_OV_FLT", "Communication supply over-voltage", "FATAL"}
    };

    private static String[][] generateRegister(String namePre, String namePost, String description, String priority) {
        String[][] res = new String[16][3];
        for (int i = 0; i < 11; i++) {
            res[i][0] = namePre + (i + 1) + namePost;
            res[i][1] = "Cell " + (i + 1) + description;
            res[i][2] = priority;
        }

        // In all generated registers the last two bits are not used and cells 12 13 14 don't exist
        // TODO: Should bits for cell 12 13 14 really be ignored
        for (int i= 11; i < res.length; i++)
            res[i] = new String[] { "", "", "NONE" };

        return res;
    }

    private static String[][] getRegister4() {
        String[][] res = new String[16][3];
        for (int i = 0; i < 16; i++) {
            res[i][0] = res[i][1] = "";
            res[i][2] = "NONE";
        }
        return res;
    }

    private static String[][] getRegister5(String priority) {
        String[][] res = new String[16][3];
        res[0] = new String[] { "", "", "NONE" };               // Ignore bit 0
        for (int i = 1; i <= 6; i++) {
            res[i][0] = "AN" + i + "_UT";
            res[i][1] = "AN " + i + " under-temperature";
            res[i][2] = priority;
        }
        res[7] = res[8] = new String[] { "", "", "NONE" };      // Ignore bit 7 8
        for (int i = 9; i <= 14; i++) {
            res[i][0] = "AN" + (i - 9 + 1) + "_OT";
            res[i][1] = "AN " + (i - 9 + 1) + " over-temperature";
            res[i][2] = priority;
        }
        res[15] = new String[] { "", "", "NONE" };              // Ignore bit 15
        return res;
    }

}