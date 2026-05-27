package nti.te4.printerkurwa.Services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nti.te4.printerkurwa.Repositories.PrinterLogRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogManagementService {

    private final PrinterLogRepository logRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void pruneLogs() {
        log.info("Running scheduled log pruning...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        long deletedCount = logRepository.deleteByTimestampBefore(threshold);
        log.info("Pruned {} logs older than 7 days.", deletedCount);
    }

    @Transactional
    public void clearLogs(UUID printerId) {
        log.info("Clearing logs for printer: {}", printerId);
        logRepository.deleteByPrinterId(printerId);
    }

    @Transactional
    public void clearAllLogs() {
        log.info("Clearing all logs from database.");
        logRepository.deleteAll();
    }

    public long getLogCount() {
        return logRepository.count();
    }
}
