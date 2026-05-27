package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.grzeslowski.jbambuapi.camera.CameraConfig;
import pl.grzeslowski.jbambuapi.camera.PSeriesCamera;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
@Service
public class BambuPSeriesCameraStrategy implements CameraStrategy {

  private static final long FRAME_INTERVAL_MS = 1000L / 5;

  @Override
  public boolean supports(String printerModel) {
    return printerModel.equalsIgnoreCase("BAMBU_P_SERIES");
  }

  @Override
  public boolean canConnect(String ip) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(ip, CameraConfig.DEFAULT_PORT), 2000);
      return true;
    } catch (Exception e) {
      log.warn("Could not connect to P-series camera at {}:{}", ip, CameraConfig.DEFAULT_PORT);
      return false;
    }
  }

  @Override
  public void streamCamera(HttpServletResponse response, String ip, String credentials) {
    if (!canConnect(ip)) {
      log.error("Camera connection check failed for P-series at {}", ip);
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    response.setContentType("multipart/x-mixed-replace; boundary=--frame");

    try (CameraConfig config = new CameraConfig(
        ip,
        CameraConfig.DEFAULT_PORT,
        CameraConfig.LOCAL_USERNAME,
        credentials.getBytes(),
        CameraConfig.BAMBU_CERTIFICATE);
        PSeriesCamera camera = new PSeriesCamera(config)) {

      camera.connect();
      log.info("Connected to P-series camera on IP: {}", ip);

      OutputStream raw = response.getOutputStream();
      BufferedOutputStream out = new BufferedOutputStream(raw, 64 * 1024);

      long nextFrameAt = System.currentTimeMillis();

      for (byte[] frame : camera) {
        if (Thread.currentThread().isInterrupted()) break;
        long now = System.currentTimeMillis();
        long wait = nextFrameAt - now;

        if (wait > 0) {
          Thread.sleep(wait);
        }

        nextFrameAt = Math.max(nextFrameAt + FRAME_INTERVAL_MS, now);

        try {
          out.write("--frame\r\n".getBytes());
          out.write("Content-Type: image/jpeg\r\n".getBytes());
          out.write(("Content-Length: " + frame.length + "\r\n\r\n").getBytes());
          out.write(frame);
          out.write("\r\n".getBytes());
          out.flush();
        } catch (java.io.IOException e) {
          log.info("Client disconnected from P-series camera stream on {}", ip);
          break;
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Camera streaming error on IP {}: {}", ip, e.getMessage());
      if (!response.isCommitted()) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }
}
