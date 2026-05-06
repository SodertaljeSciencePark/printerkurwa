package nti.te4.printerkurwa.Services;

import nti.te4.printerkurwa.Facades.PrinterFacade;
import nti.te4.printerkurwa.Facades.PrinterStatsFacade;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import nti.te4.printerkurwa.Repositories.PrinterRepository;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.UUID;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PrinterService {
  
  private final PrinterRepository printerRepository;
  private final PrinterFacade printerFacade;
  private final PrinterStatsFacade printerStatsFacade;

  public Printer addPrinter(Printer newPrinter) throws IllegalArgumentException {
    if (printerRepository.existsByIp(newPrinter.getIp())) {
      throw new IllegalArgumentException("A printer with IP: (" + newPrinter.getIp() + ") is allready registerd.");
    }

    printerFacade.checkPrinter(newPrinter);

    Printer savedPrinter = printerRepository.save(newPrinter);
    
    printerStatsFacade.startListening(savedPrinter);
    return savedPrinter;
  }

  public Printer getPrinter(UUID id) {
    return printerRepository.findById(id).orElse(null);
  }

  public Collection<Printer> getAllPrinters() {
    return printerRepository.findAll();
  }

  public Printer updatePrinter(UUID id, Printer updatedPrinter) throws IllegalArgumentException {
    Optional<Printer> existingPrinterOpt = printerRepository.findById(id);
    
    if (existingPrinterOpt.isEmpty()) {
      throw new IllegalArgumentException("Printer with ID " + id + " was not found.");
    }
    
    Printer existingPrinter = existingPrinterOpt.get();

    if (!existingPrinter.getIp().equals(updatedPrinter.getIp())) {
      if (printerRepository.existsByIpAndIdNot(updatedPrinter.getIp(), id)) {
        throw new IllegalArgumentException(
            "The IP addres " + updatedPrinter.getIp() + " is allready in use by another printer.");
      }
      printerFacade.checkPrinter(updatedPrinter);
    }

    existingPrinter.setName(updatedPrinter.getName());
    existingPrinter.setIp(updatedPrinter.getIp());
    existingPrinter.setAccessCode(updatedPrinter.getAccessCode());
    existingPrinter.setModelType(updatedPrinter.getModelType());

    return printerRepository.save(existingPrinter);
  }

  public boolean deletePrinter(UUID id) {
    if (printerRepository.existsById(id)) {
      printerRepository.deleteById(id);
      return true;
    }
    return false;
  }

  public PrinterStats getStats(UUID id) {
    return printerStatsFacade.getStats(id);
  }

  public void startListeningForPrinter(UUID id) {
    Printer printer = getPrinter(id);
    if (printer != null) {
      printerStatsFacade.startListening(printer);
    }
  }
}