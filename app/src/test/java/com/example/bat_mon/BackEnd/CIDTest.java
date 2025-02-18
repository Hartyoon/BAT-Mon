package com.example.bat_mon.BackEnd;

import com.example.bat_mon.UnitTestData;

import org.junit.Assert;
import org.junit.Test;

public class CIDTest {

    @Test
    public void CIDCellStatus_isCorrect() {
        CID cid1 = new CID(0, 0, 0);
        cid1.parseCID(UnitTestData.dataFramesExpected, 1);
        Assert.assertEquals(Cell.CellStatus.OK, cid1.getWorstVoltStatus());
        Assert.assertEquals(Cell.CellStatus.OK, cid1.getWorstTempStatus());

        CID cid2 = new CID(0, 0, 0);
        cid2.getCellAt(2).setTemp(61.0f);
        Assert.assertEquals(Cell.CellStatus.Panic, cid2.getWorstTempStatus());
        cid2.getCellAt(2).setTemp(30.0f);
        Assert.assertEquals(Cell.CellStatus.OK, cid2.getWorstTempStatus());
        cid2.getCellAt(3).setVolt(4.1f);
        Assert.assertEquals(Cell.CellStatus.High, cid2.getWorstVoltStatus());
    }

}