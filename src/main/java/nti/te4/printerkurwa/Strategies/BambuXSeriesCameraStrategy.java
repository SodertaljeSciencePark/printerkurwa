package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
@Service
public class BambuXSeriesCameraStrategy implements CameraStrategy {

  @Override
  public boolean supports(String printerModel) {
    return printerModel.equalsIgnoreCase("BAMBU_X_SERIES");
  }

  @Override
  public boolean canConnect(String ip) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(ip, 322), 2000);
      return true;
    } catch (Exception e) {
      log.warn("Could not connect to X-series camera at {}:322", ip);
      return false;
    }
  }

  @Override
  public void streamCamera(HttpServletResponse response, String ip, String credentials) {
    if (!canConnect(ip)) {
      log.error("Camera connection check failed for X-series at {}", ip);
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    avutil.av_log_set_level(avutil.AV_LOG_QUIET);
    String rtspsUrl = "rtsps://bblp:" + credentials + "@" + ip + ":322/streaming/live/1";

    response.setContentType("multipart/x-mixed-replace; boundary=--frame");

    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspsUrl);
    Java2DFrameConverter converter = new Java2DFrameConverter();

    try {
      grabber.setOption("rtsp_transport", "tcp");
      grabber.setOption("tls_verify", "0");
      grabber.setOption("stimeout", "5000000");
      grabber.setOption("loglevel", "quiet");
      grabber.setOption("fflags", "nobuffer");

      log.info("Connecting to X-series camera on: {}", ip);
      grabber.start();

      OutputStream out = response.getOutputStream();

      long lastFrameTime = 0;
      long fpsDelay = 500;

      while (!Thread.currentThread().isInterrupted()) {
        Frame frame = grabber.grabImage();
        if (frame == null) {
            Thread.sleep(10);
            continue;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFrameTime >= fpsDelay) {
          BufferedImage bufferedImage = converter.getBufferedImage(frame);
          if (bufferedImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            try {
              out.write("--frame\r\n".getBytes());
              out.write("Content-Type: image/jpeg\r\n".getBytes());
              out.write(("Content-Length: " + imageBytes.length + "\r\n\r\n").getBytes());
              out.write(imageBytes);
              out.write("\r\n".getBytes());
              out.flush();
            } catch (java.io.IOException e) {
              log.info("Client disconnected from X-series camera stream on {}", ip);
              break;
            }

            lastFrameTime = currentTime;
          }
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("X-series camera streaming error on {}: {}", ip, e.getMessage());
      if (!response.isCommitted()) {
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } finally {
        try {
            grabber.stop();
            grabber.release();
        } catch (Exception ignored) {}
        try {
            converter.close();
        } catch (Exception ignored) {}
    }
  }
}
