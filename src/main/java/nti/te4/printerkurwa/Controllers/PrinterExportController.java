package nti.te4.printerkurwa.Controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import nti.te4.printerkurwa.Repositories.PrinterRepository;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Services.PrinterService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource; 
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;    
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/printers")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class PrinterExportController {

    private final PrinterRepository printerRepository;
    private final PrinterService printerService;
    private final ObjectMapper objectMapper; 

    @GetMapping("/export/json")
    public ResponseEntity<Resource> exportPrintersToJson() {
        try {
            List<Printer> printers = printerRepository.findAll();
            
            String jsonData = objectMapper.writeValueAsString(printers);
            ByteArrayResource resource = new ByteArrayResource(jsonData.getBytes());
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "prinvue_export_" + timestamp + ".json";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);
        } catch (Exception e) {
            log.error("Downloading Printers JSON Failed, Due to: {}", e.getMessage(), e);
            throw new RuntimeException("Downloading Printers JSON Failed");
        }
    }

    @PostMapping("/import/json")
    public ResponseEntity<String> importPrintersFromJson(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a valid JSON file.");
        }
        
        try (InputStream is = file.getInputStream()) {
            List<Printer> printers = objectMapper.readValue(
                is,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Printer.class)
            );

            for (Printer p : printers) {
                p.setId(null);
                try {
                    printerService.addPrinter(p);
                } catch (Exception e) {
                    log.error("Failed to add imported printer {}: {}", p.getName(), e.getMessage());
                }
            }

            return ResponseEntity.ok("Successfully imported " + printers.size() + " printers.");
        } catch (Exception e) {
            log.error("Importing Printers JSON Failed, Due to: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Importing Printers JSON Failed");
        }
    }
}