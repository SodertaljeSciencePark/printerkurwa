package nti.te4.printerkurwa.Facades;

import lombok.extern.slf4j.Slf4j;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.Strategies.StatsStrategy;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class PrinterStatsFacade {

  private final Map<UUID, PrinterStats> statsMap = new ConcurrentHashMap<>();

  private final List<StatsStrategy> statsStrategies;

  public PrinterStats getStats(UUID printerId) {
    return statsMap.getOrDefault(printerId, new PrinterStats());
  }

  public void startListening(Printer printer) {
    stopListening(printer.getId());

    StatsStrategy strategy = statsStrategies.stream()
        .filter(s -> s.supports(printer.getModelType()))
        .findFirst()
        .orElse(null);

    if (strategy != null) {
      strategy.startListening(printer, statsMap);
    } else {
      log.error("No strategy found for: {}", printer.getModelType());
    }
  }

  public void stopListening(UUID printerId) {
    statsStrategies.forEach(s -> s.stopListening(printerId));
    statsMap.remove(printerId);
  }
}
