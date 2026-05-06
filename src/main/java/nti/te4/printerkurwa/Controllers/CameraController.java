package nti.te4.printerkurwa.Controllers;

import jakarta.servlet.http.HttpServletResponse;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Services.PrinterService;
import nti.te4.printerkurwa.Facades.PrinterFacade;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/printers")
public class CameraController {

    private final PrinterService printerService;
    private final PrinterFacade printerFacade;

    public CameraController(PrinterService printerService, PrinterFacade printerFacade) {
        this.printerService = printerService;
        this.printerFacade = printerFacade;
    }

    @GetMapping("/{id}/camera")
    public void streamCamera(@PathVariable UUID id, HttpServletResponse response) {
        Printer printer = printerService.getPrinter(id);
        if (printer == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        printerFacade.startCameraStream(printer, response);
    }
}