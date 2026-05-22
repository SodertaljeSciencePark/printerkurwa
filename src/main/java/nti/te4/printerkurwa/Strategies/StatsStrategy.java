package nti.te4.printerkurwa.Strategies;

import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import java.util.Map;
import java.util.UUID;

public interface StatsStrategy {
  boolean supports(String printerModel);

  void startListening(Printer printer, Map<UUID, PrinterStats> statsMap);

  void stopListening(UUID printerId);
}
