package nti.te4.printerkurwa.Services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SystemService {

    public void restartServices() {
        log.info("System restart requested via script.");
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                
                ProcessBuilder processBuilder = new ProcessBuilder("./scripts/restart_services.sh");
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Restart Script] " + line);
                    }
                }
                
                int exitCode = process.waitFor();
                log.info("Restart script finished with exit code: " + exitCode);
                
            } catch (Exception e) {
                log.error("Failed to execute restart script", e);
            }
        });
    }
}
