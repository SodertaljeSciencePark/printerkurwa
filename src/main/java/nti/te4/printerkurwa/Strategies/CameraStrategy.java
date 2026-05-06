package nti.te4.printerkurwa.Strategies;

import jakarta.servlet.http.HttpServletResponse;

public interface CameraStrategy {
  boolean supports(String printerModel);

  void streamCamera(HttpServletResponse response, String ip, String credentials);
}
