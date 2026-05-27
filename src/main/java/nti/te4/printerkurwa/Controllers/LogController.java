package nti.te4.printerkurwa.Controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import nti.te4.printerkurwa.Models.PrinterLog;
import nti.te4.printerkurwa.Repositories.PrinterLogRepository;
import nti.te4.printerkurwa.Services.LogManagementService;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final PrinterLogRepository printerLogRepository;
    private final LogManagementService logManagementService;

    @GetMapping("/{printerId}")
    public List<PrinterLog> getLogs(@PathVariable UUID printerId) {
        return printerLogRepository.findTop100ByPrinterIdOrderByTimestampDesc(printerId);
    }

    @DeleteMapping("/{printerId}")
    public ResponseEntity<Void> clearLogs(@PathVariable UUID printerId) {
        logManagementService.clearLogs(printerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAllLogs() {
        logManagementService.clearAllLogs();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Long> getLogStats() {
        return ResponseEntity.ok(logManagementService.getLogCount());
    }
}
