package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@Service
public class BambuXSeriesCameraStrategy implements CameraStrategy {

  @Override
  public boolean supports(String printerModel) {
    return printerModel.equalsIgnoreCase("BAMBU_X_SERIES");
  }

  @Override
  public void streamCamera(HttpServletResponse response, String ip, String credentials) {
    avutil.av_log_set_level(avutil.AV_LOG_ERROR);
    String rtspsUrl = "rtsps://bblp:" + credentials + "@" + ip + ":322/streaming/live/1";

    response.setContentType("multipart/x-mixed-replace; boundary=--frame");

    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspsUrl);
        Java2DFrameConverter converter = new Java2DFrameConverter()) {

      grabber.setOption("rtsp_transport", "tcp");
      grabber.setOption("tls_verify", "0");
      grabber.setOption("stimeout", "5000000");
      grabber.setOption("loglevel", "quiet");
      grabber.setOption("fflags", "nobuffer");

      System.out.println("Connecting to X-series camera on: " + ip);
      grabber.start();

      OutputStream out = response.getOutputStream();

      long lastFrameTime = 0;
      long fpsDelay = 500;

      while (!Thread.currentThread().isInterrupted()) {
        Frame frame = grabber.grabImage();
        if (frame == null)
          continue;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFrameTime >= fpsDelay) {
          BufferedImage bufferedImage = converter.getBufferedImage(frame);
          if (bufferedImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            out.write("--frame\r\n".getBytes());
            out.write("Content-Type: image/jpeg\r\n".getBytes());
            out.write(("Content-Length: " + imageBytes.length + "\r\n\r\n").getBytes());
            out.write(imageBytes);
            out.write("\r\n".getBytes());
            out.flush();

            lastFrameTime = currentTime;
          }
        }
      }

      grabber.stop();
    } catch (Exception e) {
      System.err.println("X-series camera streaming error: " + e.getMessage());
    }
  }
}
