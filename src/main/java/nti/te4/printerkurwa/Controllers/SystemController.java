package nti.te4.printerkurwa.Controllers;

import nti.te4.printerkurwa.Services.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @PostMapping("/restart")
    public ResponseEntity<String> restart() {
        systemService.restartServices();
        return ResponseEntity.ok("Restart sequence initiated.");
    }
}
