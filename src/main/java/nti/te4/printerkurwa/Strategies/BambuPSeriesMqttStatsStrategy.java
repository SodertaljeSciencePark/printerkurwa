package nti.te4.printerkurwa.Strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import nti.te4.printerkurwa.Models.Printer;
import nti.te4.printerkurwa.Models.PrinterStats;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import nti.te4.printerkurwa.libs.Helpers.Rounders;

@Slf4j
@Service
public class BambuPSeriesMqttStatsStrategy implements StatsStrategy {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<UUID, Mqtt3AsyncClient> activeClients = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String printerModel) {
        if (printerModel == null) return false;
        return printerModel.equalsIgnoreCase("BAMBU_P_SERIES");
    }

    @Override
    public void startListening(Printer printer, Map<UUID, PrinterStats> statsMap) {
        stopListening(printer.getId());

        log.info("Starting P-Series MQTT stats listener for: {} ({})", printer.getName(), printer.getSerial());

        String serial = printer.getSerial();
        if (serial == null || serial.trim().isEmpty()) {
            log.error("Could not start MQTT: serial number is missing for printer: {}", printer.getName());
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
                    .automaticReconnect()
                        .initialDelay(1, java.util.concurrent.TimeUnit.SECONDS)
                        .maxDelay(60, java.util.concurrent.TimeUnit.SECONDS)
                        .applyAutomaticReconnect()
                    .buildAsync();

            client.connectWith()
                    .simpleAuth()
                        .username("bblp")
                        .password(printer.getAccessCode().getBytes())
                        .applySimpleAuth()
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            log.error("--- COULD NOT START MQTT FOR BAMBU P-SERIES ---", throwable);
                            return;
                        }

                        activeClients.put(printer.getId(), client);

                        String topic = "device/" + serial + "/report";

                        client.subscribeWith()
                                .topicFilter(topic)
                                .qos(MqttQos.AT_LEAST_ONCE)
                                .callback(publish -> handleMessage(publish, printer, statsMap))
                                .send()
                                .whenComplete((subAck, subThrowable) -> {
                                    if (subThrowable != null) {
                                        log.error("Failed to subscribe to topic {} for printer {}: {}", topic, printer.getName(), subThrowable.getMessage());
                                    } else {
                                        log.info("Linked! Listening on P-Series stats from: {} on topic: {}", printer.getIp(), topic);
                                    }
                                });
                    });

        } catch (Exception e) {
            log.error("--- COULD NOT START MQTT FOR BAMBU P-SERIES ---", e);
        }
    }

    @Override
    public void stopListening(UUID printerId) {
        Mqtt3AsyncClient client = activeClients.remove(printerId);
        if (client != null) {
            log.info("Stopping P-Series MQTT listener for printer ID: {}", printerId);
            client.disconnect();
        }
    }

    private void handleMessage(Mqtt3Publish publish, Printer printer, Map<UUID, PrinterStats> statsMap) {
        try {
            String payload = new String(publish.getPayloadAsBytes());
            JsonNode root = mapper.readTree(payload);

            if (root.has("print")) {
                JsonNode print = root.get("print");
                PrinterStats currentStats = statsMap.getOrDefault(printer.getId(), new PrinterStats());
                currentStats.setLastUpdated(System.currentTimeMillis());
                currentStats.setOnline(true);

                if (print.has("bed_temper"))
                    currentStats.setBedTemp(Rounders.round(print.get("bed_temper").asDouble(), 1));
                if (print.has("nozzle_temper"))
                    currentStats.setNozzleTemp(Rounders.round(print.get("nozzle_temper").asDouble(), 1));
                if (print.has("mc_percent"))
                    currentStats.setProgressPercent(print.get("mc_percent").asInt());
                if (print.has("gcode_state"))
                    currentStats.setCurrentStatus(print.get("gcode_state").asText());

                statsMap.put(printer.getId(), currentStats);
            }
        } catch (Exception e) {
            log.error("Error occurred while handling MQTT message for P-Series printer: {}", printer.getName(), e);
        }
    }
}
