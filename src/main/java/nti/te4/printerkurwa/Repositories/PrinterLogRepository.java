package nti.te4.printerkurwa.Repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nti.te4.printerkurwa.Models.PrinterLog;

@Repository
public interface PrinterLogRepository extends JpaRepository<PrinterLog, UUID> {
    List<PrinterLog> findTop20ByPrinterIdOrderByTimestampDesc(UUID printerId);
    List<PrinterLog> findByPrinterIdOrderByTimestampDesc(UUID printerId);
    List<PrinterLog> findTop100ByPrinterIdOrderByTimestampDesc(UUID printerId);
    long deleteByTimestampBefore(LocalDateTime threshold);
    void deleteByPrinterId(UUID printerId);
}
