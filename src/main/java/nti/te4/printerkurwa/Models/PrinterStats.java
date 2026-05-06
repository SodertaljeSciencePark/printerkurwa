package nti.te4.printerkurwa.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterStats {
  public double bedTemp;
  public double nozzleTemp;
  public int progressPercent;
  public String currentStatus;

}
