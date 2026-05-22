package nti.te4.printerkurwa.Strategies;

import lombok.extern.slf4j.Slf4j;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.libs.prusalink.PrusaLinkClient;
import nti.te4.printerkurwa.libs.prusalink.PrusaStatus;
import nti.te4.printerkurwa.libs.prusalink.PrusaConnectionException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PrusaStatsStrategy implements StatsStrategy {

    private final Map<UUID, Thread> activePollers = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String printerModel) {
        return printerModel != null && printerModel.toUpperCase().contains("PRUSA");
    }

    @Override
    public void startListening(Printer printer, Map<UUID, PrinterStats> statsMap) {
        stopListening(printer.getId());

        log.info("Starting Prusa stats poller for: {}", printer.getName());

        String ip = printer.getIp();
        if (ip == null || ip.trim().isEmpty()) {
            log.error("Could not start Prusa poller: IP is missing for printer: {}", printer.getName());
            return;
        }

        Thread pollerThread = new Thread(() -> {
            PrusaLinkClient client = new PrusaLinkClient(ip, "maker", printer.getAccessCode());
            int errorCount = 0;
            long pollInterval = 10_000;
            final long maxInterval = 60_000;

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        PrusaStatus status = client.getStatus();

                        PrinterStats currentStats = statsMap.getOrDefault(printer.getId(), new PrinterStats());
                        currentStats.setCurrentStatus(status.getState());
                        currentStats.setProgressPercent((int) status.getProgress());
                        currentStats.setBedTemp(status.getTempAmbient());
                        currentStats.setNozzleTemp(status.getTempUvLed());

                        statsMap.put(printer.getId(), currentStats);

                        errorCount = 0;
                        pollInterval = 10_000;

                    } catch (PrusaConnectionException e) {
                        errorCount++;
                        if (errorCount <= 3) {
                            log.error("Prusa polling error for {}: {}", printer.getName(), e.getMessage());
                        } else if (errorCount == 4) {
                            log.warn("Prusa poller for {} backing off silently.", printer.getName());
                        }
                        pollInterval = Math.min(pollInterval * 2, maxInterval);

                    } catch (Exception e) {
                        errorCount++;
                        if (errorCount <= 3) {
                            log.error("Unexpected error polling Prusa stats for {}: {}", printer.getName(), e.getMessage());
                        }
                        pollInterval = Math.min(pollInterval * 2, maxInterval);
                    }

                    Thread.sleep(pollInterval);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activePollers.remove(printer.getId());
                log.info("Prusa stats poller stopped for: {}", printer.getName());
            }
        });

        pollerThread.setDaemon(true);
        pollerThread.setName("PrusaPoller-" + printer.getId());
        activePollers.put(printer.getId(), pollerThread);
        pollerThread.start();

        log.info("Prusa stats poller started for: {}", printer.getName());
    }

    @Override
    public void stopListening(UUID printerId) {
        Thread thread = activePollers.get(printerId);
        if (thread != null && thread.isAlive()) {
            log.info("Stopping Prusa stats poller for printer ID: {}", printerId);
            thread.interrupt();
            activePollers.remove(printerId);
        }
    }
    
}
