package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;

public interface CameraStrategy {
  boolean supports(String printerModel);

  boolean canConnect(String ip);

  void streamCamera(HttpServletResponse response, String ip, String credentials);
}
