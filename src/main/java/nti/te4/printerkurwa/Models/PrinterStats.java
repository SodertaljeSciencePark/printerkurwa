package nti.te4.printerkurwa.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrinterStats {
  public double bedTemp;
  public double nozzleTemp;
  public int progressPercent;
  public String currentStatus;
  
  @Builder.Default
  public long lastUpdated = System.currentTimeMillis();
  
  @Builder.Default
  public boolean isOnline = false;

  public static PrinterStats offline() {
      return PrinterStats.builder()
          .currentStatus("OFFLINE")
          .isOnline(false)
          .lastUpdated(System.currentTimeMillis())
          .build();
  }
}
