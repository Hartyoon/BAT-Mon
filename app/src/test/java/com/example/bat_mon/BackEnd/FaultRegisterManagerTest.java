package com.example.bat_mon.BackEnd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.bat_mon.BackEnd.BatMonError.Priority;
import com.example.bat_mon.BackEnd.FaultRegisterLookup.RegisterNames;

import org.junit.Assert;
import org.junit.Test;

public class FaultRegisterManagerTest {

    /**
     * Checks if the FR lookup arrays are correctly generated.
     */
    @Test
    public void faultRegisterLookup_isCorrect() {
        for (int i = 0; i < FaultRegisterManager.registerCount; i++) {
            RegisterNames regName = RegisterNames.values()[i];
            Assert.assertNotNull(regName);
            System.out.print(regName + " ");

            String[][] lookup = FaultRegisterLookup.getRegister(regName);
            Assert.assertNotNull(lookup);
            for (int j = 0; j < lookup.length; j++) {
                System.out.print(j + " ");
                assertEquals(3, lookup[j].length); // A lookup pair should always be { NAME, DESCRIPTION, PRIORITY } --> length 3

                // No lookup string should be null
                Assert.assertNotNull(lookup[j][0]);
                Assert.assertNotNull(lookup[j][1]);
                Assert.assertNotNull(lookup[j][2]);

                // Check if priority string is correct (e.g. typos), throws IllegalArgumentException when not
                Priority.valueOf(lookup[i][2]);
            }
            System.out.print("\n");
        }
    }

    @Test
    public void balacingTimer_isCorrect() throws InterruptedException{
        FaultRegisterManager frm = new FaultRegisterManager(0, 0);
        FaultRegisterManager.secondsToError = 1;
        int[] frUpdate = new int[9];
        frUpdate[2] = 0xFFFF; //Balancing Faults to 1
        frm.updateFaultRegister(frUpdate); //update the faults
        Thread.sleep(FaultRegisterManager.secondsToError * 2000);
        frUpdate[2] = 0; //set all bits to 0 (balancing starts)
        frm.updateFaultRegister(frUpdate); //The Start-timers should now have been updated
        Thread.sleep(FaultRegisterManager.secondsToError * 2000);
        frUpdate[2] = 0xFFFF; //to finish the balancing cycle, all bits to 1 again
        frm.updateFaultRegister(frUpdate); //the time should now have been updated

        long[] expectedBalancingTimes = {2,2,2,2,2,2,2,2,2,2,2};
        Assert.assertArrayEquals(expectedBalancingTimes ,frm.getBalancingTimes()); //cells have balanced 2 seconds

        frm.updateFaultRegister(frUpdate); //update again with the same values
        expectedBalancingTimes = new long[11];
        Assert.assertArrayEquals(expectedBalancingTimes ,frm.getBalancingTimes()); //should now return 0 since cells arent balancing anymore
    }


    @Test
    public void updateFaultRegisters_isCorrect() throws InterruptedException {
        ErrorHandler.getErrorList().clear();
        FaultRegisterManager frm = new FaultRegisterManager(0, 0);
        FaultRegisterManager.secondsToError = 1;

        // All fault registers 0 --> No Errors in error list
        int[] frUpdate = new int[9];
        frm.updateFaultRegister(frUpdate);
        assertTrue(ErrorHandler.getErrorList().isEmpty());

        // Set first bit in first fault register to 1 --> Should generate error
        frUpdate[0] = 1;
        frm.updateFaultRegister(frUpdate);
        Assert.assertEquals(0, ErrorHandler.getErrorList().size()); // There should only be an error after we waited for long enough
        Thread.sleep(FaultRegisterManager.secondsToError * 1000);
        frm.updateFaultRegister(frUpdate);

        BatMonError error = ErrorHandler.getLatestError();
        Assert.assertEquals("Seg 0 CID 0 FAULT1_STATUS: CT_UV_FLT: Under-voltage was detected", error.getMessage());
        Assert.assertEquals(BatMonError.ErrorCode.FAULT_REGISTER_FAULT, error.getErrorCode());
        Assert.assertEquals(Priority.FATAL, error.getPriority());
        Assert.assertEquals(BatMonError.ErrorType.FAULT_REGISTER, error.getErrorType());

        // Set first bit in FAULT3_STATUS register to 1 --> Should generate new error
        frUpdate[2] = 1;
        frm.updateFaultRegister(frUpdate);
        Assert.assertEquals(1, ErrorHandler.getErrorList().size()); // There should only be an error after we waited for long enough
        Thread.sleep(FaultRegisterManager.secondsToError * 1000);
        frm.updateFaultRegister(frUpdate);

        BatMonError error2 = ErrorHandler.getLatestError();
        Assert.assertEquals("Seg 0 CID 0 FAULT3_STATUS: EOT_CB1: Cell 1 balancing based on timer done", error2.getMessage());
        Assert.assertEquals(BatMonError.ErrorCode.FAULT_REGISTER_FAULT, error2.getErrorCode());
        Assert.assertEquals(Priority.INFO, error2.getPriority());
        Assert.assertEquals(BatMonError.ErrorType.FAULT_REGISTER, error2.getErrorType());

        // We should have 2 errors now, since only updated registers should generate new errors
        Assert.assertEquals(2, ErrorHandler.getErrorList().size());

        // Even when we update the registers with the same state again, we should not generate the same error message twice
        frm.updateFaultRegister(frUpdate);
        Thread.sleep(FaultRegisterManager.secondsToError * 1000);
        frm.updateFaultRegister(frUpdate);
        Assert.assertEquals(2, ErrorHandler.getErrorList().size());

        // Now we set the error bit to 0 again, update, then set it to 1 again, and we should generate a new error message
        frUpdate[2] = 0;
        frm.updateFaultRegister(frUpdate);
        frUpdate[2] = 1;
        frm.updateFaultRegister(frUpdate);
        Thread.sleep(FaultRegisterManager.secondsToError * 1000);
        frm.updateFaultRegister(frUpdate);
        Assert.assertEquals(3, ErrorHandler.getErrorList().size());
    }

}