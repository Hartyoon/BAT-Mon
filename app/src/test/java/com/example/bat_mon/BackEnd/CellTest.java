package com.example.bat_mon.BackEnd;

import static org.junit.Assert.assertEquals;

import com.example.bat_mon.BackEnd.Cell.CellStatus;

import org.junit.Test;

public class CellTest {

    @Test
    public void cellStatus_isCorrect() {
        Cell c1 = new Cell(0);
        assertEquals(CellStatus.OK, c1.tempStatus);
        assertEquals(CellStatus.OK, c1.voltStatus);

        c1.setTemp(-51.0f); // Error low
        assertEquals(CellStatus.Error, c1.tempStatus);
        c1.setTemp(-40.0f); // Panic low
        assertEquals(CellStatus.Error, c1.tempStatus);
        c1.setTemp(-1.0f); // Warn low
        assertEquals(CellStatus.Error, c1.tempStatus);
        c1.setTemp(46.0f); // Warn high
        assertEquals(CellStatus.High, c1.tempStatus);
        c1.setTemp(61.0f); // Panic high
        assertEquals(CellStatus.Panic, c1.tempStatus);
        c1.setTemp(200.0f); // Error high
        assertEquals(CellStatus.Error, c1.tempStatus);
        c1.setTemp(30.0f); // OK
        assertEquals(CellStatus.OK, c1.tempStatus);

        c1.setVolt(-1.0f); // Error low
        assertEquals(CellStatus.Error, c1.voltStatus);
        c1.setVolt(0.1f); // Panic low
        assertEquals(CellStatus.Panic, c1.voltStatus);
        c1.setVolt(3.3f); // Warn low
        assertEquals(CellStatus.Low, c1.voltStatus);
        c1.setVolt(4.1f); // Warn high
        assertEquals(CellStatus.High, c1.voltStatus);
        c1.setVolt(4.5f); // Panic high
        assertEquals(CellStatus.Panic, c1.voltStatus);
        c1.setVolt(11.0f); // Error high
        assertEquals(CellStatus.Error, c1.voltStatus);
        c1.setVolt(3.7f); // OK
        assertEquals(CellStatus.OK, c1.voltStatus);
    }

}