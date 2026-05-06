package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import pl.grzeslowski.jbambuapi.camera.CameraConfig;
import pl.grzeslowski.jbambuapi.camera.PSeriesCamera;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

@Service
public class BambuPSeriesCameraStrategy implements CameraStrategy {

  private static final long FRAME_INTERVAL_MS = 1000L / 5;

  @Override
  public boolean supports(String printerModel) {
    return printerModel.equalsIgnoreCase("BAMBU_P_SERIES");
  }

  @Override
  public void streamCamera(HttpServletResponse response, String ip, String credentials) {
    response.setContentType("multipart/x-mixed-replace; boundary=--frame");

    try (CameraConfig config = new CameraConfig(
        ip,
        CameraConfig.DEFAULT_PORT,
        CameraConfig.LOCAL_USERNAME,
        credentials.getBytes(),
        CameraConfig.BAMBU_CERTIFICATE);
        PSeriesCamera camera = new PSeriesCamera(config)) {

      camera.connect();
      System.out.println("Connected to P-series camera on IP: " + ip);

      OutputStream raw = response.getOutputStream();
      BufferedOutputStream out = new BufferedOutputStream(raw, 64 * 1024);

      long nextFrameAt = System.currentTimeMillis();

      for (byte[] frame : camera) {
        long now = System.currentTimeMillis();
        long wait = nextFrameAt - now;

        if (wait > 0) {
          Thread.sleep(wait);
        }

        nextFrameAt = Math.max(nextFrameAt + FRAME_INTERVAL_MS, now);

        out.write("--frame\r\n".getBytes());
        out.write("Content-Type: image/jpeg\r\n".getBytes());
        out.write(("Content-Length: " + frame.length + "\r\n\r\n").getBytes());
        out.write(frame);
        out.write("\r\n".getBytes());
        out.flush();
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      System.err.println("Camera streaming error: " + e.getMessage());
    }
  }
}
