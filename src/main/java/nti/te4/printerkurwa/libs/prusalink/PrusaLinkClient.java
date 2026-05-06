package nti.te4.printerkurwa.libs.prusalink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrusaLinkClient {

  private final String ipAddress;
  private final String username;
  private final String password;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  private String realm;
  private String nonce;
  private String opaque;
  private String qop;
  private int nonceCount = 0;

  private static final Pattern DIGEST_PARAM_PATTERN = Pattern.compile(
      "(\\w+)=\"([^\"]*)\"|(\\w+)=(\\S+)");

  public PrusaLinkClient(String ipAddress, String username, String password) {
    this.ipAddress = ipAddress;
    this.username = username;
    this.password = password;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
  }

  public PrusaStatus getStatus() throws PrusaConnectionException {
    try {
      String url = "http://" + ipAddress + "/api/v1/status";

      HttpRequest initialRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Accept", "application/json")
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();

      HttpResponse<String> initialResponse = httpClient.send(initialRequest, HttpResponse.BodyHandlers.ofString());

      if (initialResponse.statusCode() == 200) {
        return parseJsonToStatus(initialResponse.body());
      }

      if (initialResponse.statusCode() != 401) {
        throw new PrusaConnectionException("API returnerade felkod: " + initialResponse.statusCode(), null);
      }

      String wwwAuth = initialResponse.headers().firstValue("WWW-Authenticate").orElse("");
      parseDigestChallenge(wwwAuth);

      String authHeader = buildDigestHeader("GET", url);

      HttpRequest authRequest = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Accept", "application/json")
          .header("Authorization", authHeader)
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();

      HttpResponse<String> authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString());

      if (authResponse.statusCode() != 200) {
        throw new PrusaConnectionException(
            "Authentication failed, API returnerade felkod: " + authResponse.statusCode(), null);
      }

      return parseJsonToStatus(authResponse.body());

    } catch (PrusaConnectionException e) {
      throw e;
    } catch (Exception e) {
      throw new PrusaConnectionException("Kunde inte ansluta till Prusa på " + ipAddress, e);
    }
  }

  private void parseDigestChallenge(String wwwAuth) throws PrusaConnectionException {
    if (wwwAuth == null || wwwAuth.isBlank()) {
      throw new PrusaConnectionException("Tomt WWW-Authenticate-huvud", null);
    }

    Map<String, String> params = new HashMap<>();
    Matcher matcher = DIGEST_PARAM_PATTERN.matcher(wwwAuth);
    while (matcher.find()) {
      String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
      String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
      if (key != null)
        params.put(key.toLowerCase(), value);
    }

    this.realm = params.get("realm");
    this.nonce = params.get("nonce");
    this.opaque = params.get("opaque");
    this.qop = params.get("qop");
    this.nonceCount = 0;

    if (this.realm == null || this.nonce == null) {
      throw new PrusaConnectionException("Digest challenge saknar realm eller nonce: " + wwwAuth, null);
    }
  }

  private String buildDigestHeader(String method, String url) throws Exception {
    String uri = new URI(url).getPath();
    if (uri.isEmpty())
      uri = "/";

    String ha1 = md5(username + ":" + realm + ":" + password);
    String cnonce = generateCnonce();
    nonceCount++;
    String nc = String.format("%08x", nonceCount);

    String ha2;
    String responseDigest;

    if (qop != null && qop.equals("auth")) {
      ha2 = md5(method + ":" + uri);
      responseDigest = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
    } else {
      ha2 = md5(method + ":" + uri);
      responseDigest = md5(ha1 + ":" + nonce + ":" + ha2);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Digest username=\"").append(username).append("\"");
    sb.append(", realm=\"").append(realm).append("\"");
    sb.append(", nonce=\"").append(nonce).append("\"");
    sb.append(", uri=\"").append(uri).append("\"");
    sb.append(", response=\"").append(responseDigest).append("\"");
    if (opaque != null) {
      sb.append(", opaque=\"").append(opaque).append("\"");
    }
    if (qop != null) {
      sb.append(", qop=").append(qop);
      sb.append(", nc=").append(nc);
      sb.append(", cnonce=\"").append(cnonce).append("\"");
    }

    return sb.toString();
  }

  private String md5(String input) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private String generateCnonce() {
    return Long.toHexString(Double.doubleToLongBits(Math.random()));
  }

  private PrusaStatus parseJsonToStatus(String json) throws Exception {
      JsonNode root = objectMapper.readTree(json);
      PrusaStatus status = new PrusaStatus();

      JsonNode printer = root.path("printer");

      if (!printer.path("state").isMissingNode())
          status.setState(printer.path("state").asText());
      if (!printer.path("temp_ambient").isMissingNode())
          status.setTempAmbient(printer.path("temp_ambient").asDouble());
      if (!printer.path("temp_cpu").isMissingNode())
          status.setTempCpu(printer.path("temp_cpu").asDouble());
      if (!printer.path("temp_uv_led").isMissingNode())
          status.setTempUvLed(printer.path("temp_uv_led").asDouble());
      if (!printer.path("fan_blower").isMissingNode())
          status.setFanBlower(printer.path("fan_blower").asInt());
      if (!printer.path("fan_rear").isMissingNode())
          status.setFanRear(printer.path("fan_rear").asInt());
      if (!printer.path("fan_uv_led").isMissingNode())
          status.setFanUvLed(printer.path("fan_uv_led").asInt());
      if (!printer.path("cover_closed").isMissingNode())
          status.setCoverClosed(printer.path("cover_closed").asBoolean());

      JsonNode progressNode = root.path("job").path("progress");
      if (!progressNode.isMissingNode())
          status.setProgress(progressNode.asDouble());

      return status;
  }
}
