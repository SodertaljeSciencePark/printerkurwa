package nti.te4.printerkurwa.Controllers;

import nti.te4.printerkurwa.Facades.PrinterStatsFacade;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.Services.PrinterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/printers")
@AllArgsConstructor
public class PrinterController {

  private final PrinterService printerService;
  private final PrinterStatsFacade printerStatsFacade;

  @PostMapping
  public ResponseEntity<?> addPrinter(@RequestBody Printer printer) {
    try {
      Printer savedPrinter = printerService.addPrinter(printer);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedPrinter);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred");
    }
  }

  @GetMapping
  public Collection<Printer> getAllPrinters() {
    return printerService.getAllPrinters();
  }

  @GetMapping("/{id}/stats")
  public ResponseEntity<?> getPrinterStats(@PathVariable UUID id) {
    PrinterStats stats = printerStatsFacade.getStats(id);
    return ResponseEntity.ok(stats);
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> updatePrinter(@PathVariable UUID id, @RequestBody Printer updatedPrinter) {
    try {
      Printer savedPrinter = printerService.updatePrinter(id, updatedPrinter);
      return ResponseEntity.ok(savedPrinter);

    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("Not found")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating");
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePrinter(@PathVariable UUID id) {
    boolean deleted = printerService.deletePrinter(id);

    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Printer with ID " + id + " was not found");
    }
  }
}
