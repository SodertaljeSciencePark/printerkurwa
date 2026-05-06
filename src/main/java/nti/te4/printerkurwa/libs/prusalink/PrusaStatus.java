package nti.te4.printerkurwa.libs.prusalink;

import lombok.Data;

@Data
public class PrusaStatus {
    private String state;
    private double progress;
    private double tempAmbient;
    private double tempCpu;
    private double tempUvLed;
    private int fanBlower;
    private int fanRear;
    private int fanUvLed;
    private boolean coverClosed;
}