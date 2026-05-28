package nti.te4.printerkurwa.Services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nti.te4.printerkurwa.Facades.PrinterStatsFacade;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterLog;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.Repositories.PrinterLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogRecorderService {

    private final PrinterService printerService;
    private final PrinterStatsFacade statsFacade;
    private final PrinterLogRepository logRepository;

    @PostConstruct
    public void init() {
        log.info("LogRecorderService initialized. Starting telemetry listeners...");
        try {
            Collection<Printer> printers = printerService.getAllPrinters();
            for (Printer printer : printers) {
                statsFacade.startListening(printer);
            }
        } catch (Exception e) {
            log.error("Failed to start telemetry listeners on startup: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void recordPrinterSnapshots() {
        try {
            Collection<Printer> printers = printerService.getAllPrinters();

            for (Printer printer : printers) {
                try {
                    PrinterStats stats = statsFacade.getStats(printer.getId());
                    
                    if (stats != null && stats.isOnline() && stats.getCurrentStatus() != null) {
                        PrinterLog historyEntry = PrinterLog.builder()
                                .printerId(printer.getId())
                                .eventType("TELEMETRY_SNAPSHOT")
                                .message("Status: " + stats.getCurrentStatus() + ", Progress: " + stats.getProgressPercent() + "%")
                                .nozzleTemp(stats.getNozzleTemp())
                                .bedTemp(stats.getBedTemp())
                                .timestamp(LocalDateTime.now())
                                .build();

                        logRepository.save(historyEntry);
                    }
                } catch (Exception e) {
                    log.error("Error recording snapshot for printer {}: {}", printer.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Global error in recordPrinterSnapshots: {}", e.getMessage());
        }
    }
}
