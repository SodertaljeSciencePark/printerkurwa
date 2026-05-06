package nti.te4.printerkurwa.Strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class BambuPSeriesMqttStatsStrategy implements StatsStrategy {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(String printerModel) {
        if (printerModel == null) return false;
        return printerModel.equalsIgnoreCase("BAMBU_P_SERIES");
    }

    @Override
    public void startListening(Printer printer, Map<UUID, PrinterStats> statsMap) {
        System.out.println("Starting P-Series MQTT stats listener for: " + printer.getName() + " (" + printer.getSerial() + ")");

        String serial = printer.getSerial();
        if (serial == null || serial.trim().isEmpty()) {
            System.err.println("Could not start MQTT: serial number is missing for printer: " + printer.getName());
            return;
        }

        try {
            Mqtt3AsyncClient client = MqttClient.builder()
                    .useMqttVersion3()
                    .identifier("BambuDash_" + UUID.randomUUID().toString().substring(0, 8))
                    .serverHost(printer.getIp())
                    .serverPort(8883)
                    .sslConfig()
                        .trustManagerFactory(InsecureTrustManagerFactory.INSTANCE)
                        .applySslConfig()
                    .buildAsync();

            client.connectWith()
                    .simpleAuth()
                        .username("bblp")
                        .password(printer.getAccessCode().getBytes())
                        .applySimpleAuth()
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("--- KUNDE INTE STARTA MQTT FÖR BAMBU P-SERIES ---");
                            throwable.printStackTrace();
                            return;
                        }

                        String topic = "device/" + serial + "/report";

                        client.subscribeWith()
                                .topicFilter(topic)
                                .qos(MqttQos.AT_LEAST_ONCE)
                                .callback(publish -> handleMessage(publish, printer, statsMap))
                                .send()
                                .whenComplete((subAck, subThrowable) -> {
                                    if (subThrowable != null) {
                                        System.err.println("Failed to subscribe to topic " + topic + ": " + subThrowable.getMessage());
                                    } else {
                                        System.out.println("Linked! Listening on P-Series stats from: " + printer.getIp() + " on topic: " + topic);
                                    }
                                });
                    });

        } catch (Exception e) {
            System.err.println("--- KUNDE INTE STARTA MQTT FÖR BAMBU P-SERIES ---");
            e.printStackTrace();
        }
    }

    private void handleMessage(Mqtt3Publish publish, Printer printer, Map<UUID, PrinterStats> statsMap) {
        try {
            String payload = new String(publish.getPayloadAsBytes());
            JsonNode root = mapper.readTree(payload);

            if (root.has("print")) {
                JsonNode print = root.get("print");
                PrinterStats currentStats = statsMap.getOrDefault(printer.getId(), new PrinterStats());

                if (print.has("bed_temper"))
                    currentStats.setBedTemp(print.get("bed_temper").asDouble());
                if (print.has("nozzle_temper"))
                    currentStats.setNozzleTemp(print.get("nozzle_temper").asDouble());
                if (print.has("mc_percent"))
                    currentStats.setProgressPercent(print.get("mc_percent").asInt());
                if (print.has("gcode_state"))
                    currentStats.setCurrentStatus(print.get("gcode_state").asText());

                statsMap.put(printer.getId(), currentStats);
            }
        } catch (Exception ignored) {
            System.err.println("Error occurred while handling MQTT message for P-Series printer: " + printer.getName());
            ignored.printStackTrace();
        }
    }
}
