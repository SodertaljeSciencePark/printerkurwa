package nti.te4.printerkurwa.Strategies;

import java.util.Map;
import java.util.UUID;

import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.libs.prusalink.PrusaLinkClient;
import nti.te4.printerkurwa.libs.prusalink.PrusaStatus;
import nti.te4.printerkurwa.libs.prusalink.PrusaConnectionException;
import org.springframework.stereotype.Service;

@Service
public class PrusaStatsStrategy implements StatsStrategy {
    @Override
    public boolean supports(String printerModel) {
        return printerModel != null && printerModel.toUpperCase().contains("PRUSA");
    }

    @Override
    public void startListening(Printer printer, Map<UUID, PrinterStats> statsMap) {
        System.out.println("Starting Prusa stats poller for: " + printer.getName());

        String ip = printer.getIp();
        if (ip == null || ip.trim().isEmpty()) {
            System.err.println("Could not start Prusa poller: IP is missing for printer: " + printer.getName());
            return;
        }

        Thread pollerThread = new Thread(() -> {
            PrusaLinkClient client = new PrusaLinkClient(ip, "maker", printer.getAccessCode());
            int errorCount = 0;
            long pollInterval = 10_000;
            final long maxInterval = 60_000;

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
                        System.err.println("Prusa polling error for " + printer.getName() + ": " + e.getMessage());
                    } else if (errorCount == 4) {
                        System.err.println("Prusa poller for " + printer.getName() + " backing off silently.");
                    }
                    pollInterval = Math.min(pollInterval * 2, maxInterval);

                } catch (Exception e) {
                    errorCount++;
                    if (errorCount <= 3) {
                        System.err.println("Unexpected error polling Prusa stats: " + e.getMessage());
                    }
                    pollInterval = Math.min(pollInterval * 2, maxInterval);
                }

                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        pollerThread.setDaemon(true);
        pollerThread.setName("PrusaPoller-" + printer.getId());
        pollerThread.start();

        System.out.println("Prusa stats poller started for: " + printer.getName());
    }
    
}
