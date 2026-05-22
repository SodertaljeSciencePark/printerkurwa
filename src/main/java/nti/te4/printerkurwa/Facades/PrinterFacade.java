package nti.te4.printerkurwa.Facades;

import jakarta.servlet.http.HttpServletResponse;
import nti.te4.printerkurwa.Strategies.CameraStrategy;
import nti.te4.printerkurwa.Models.Printer;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;

@Service
public class PrinterFacade {

  private final List<CameraStrategy> cameraStrategies;

  public PrinterFacade(List<CameraStrategy> cameraStrategies) {
    this.cameraStrategies = cameraStrategies;
  }

  public void startCameraStream(Printer printer, HttpServletResponse response) {
      CameraStrategy strategy = cameraStrategies.stream()
              .filter(s -> s.supports(printer.getModelType()))
              .findFirst()
              .orElse(null);

      if (strategy == null) {
          try {
              response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                  "Camera is not supported on model: " + printer.getModelType());
          } catch (Exception ignored) {}
          return;
      }

      if (!strategy.canConnect(printer.getIp())) {
          try {
              response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                  "Could not connect to camera at " + printer.getIp());
          } catch (Exception ignored) {}
          return;
      }

      strategy.streamCamera(response, printer.getIp(), printer.getAccessCode());
  }

  public void checkPrinter(Printer printer) {
    try {
      InetAddress address = InetAddress.getByName(printer.getIp());
      if (!address.isReachable(2000)) {
        throw new IllegalArgumentException("Could not find printer " + printer.getIp());
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid ip or network problem: " + e.getMessage());
    }
  }
}
