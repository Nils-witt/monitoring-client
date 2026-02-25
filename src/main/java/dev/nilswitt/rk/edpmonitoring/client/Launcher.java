package dev.nilswitt.rk.edpmonitoring.client;

import dev.nilswitt.rk.edpmonitoring.client.helpers.TrayHelper;
import dev.nilswitt.rk.edpmonitoring.client.structs.RemoteSystem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Launcher {

    private static final Logger log = LogManager.getLogger();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");// 2026-02-16_01-21-16

    ObjectMapper mapper = new ObjectMapper();

    private final Properties config = loadConfig();

    private static Properties loadConfig() {
        Properties props = new Properties();

        // 1. Try loading from the directory next to the running JAR
        try {
            Path jarDir = Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            Path externalConfig = jarDir.resolve("application.properties");
            if (externalConfig.toFile().exists()) {
                try (InputStream is = new FileInputStream(externalConfig.toFile())) {
                    props.load(is);
                    log.info("Loaded application.properties from {}", externalConfig);
                }
            }
        } catch (URISyntaxException | IOException e) {
            log.warn("Could not load external application.properties: {}", e.getMessage());
        }

        // 2. Fall back to the bundled classpath resource if nothing was loaded externally
        if (props.isEmpty()) {
            try (InputStream is = Launcher.class.getResourceAsStream("/application.properties")) {
                if (is != null) {
                    props.load(is);
                    log.info("Loaded application.properties from classpath");
                } else {
                    log.warn("application.properties not found on classpath, using defaults");
                }
            } catch (IOException e) {
                log.error("Failed to load application.properties from classpath", e);
            }
        }

        if (props.isEmpty()) {
            log.warn("No application.properties found anywhere, using defaults");
        }
        for (String key : props.stringPropertyNames()) {
            log.debug("Config: {} = {}", key, props.getProperty(key));
        }
        return props;
    }

    public List<RemoteSystem> getRemoteSystems() {
        log.info("Getting remote systems");
        Set<Integer> indices = new HashSet<>();
        config.propertyNames().asIterator().forEachRemaining(key -> {
            String keyStr = (String) key;
            if (keyStr.startsWith("app.remote.")) {
                log.info(keyStr);
                String[] parts = keyStr.split("\\.");
                indices.add(Integer.parseInt(parts[2]));
            }
        });


        return indices.stream().map(integer -> {
            String name = config.getProperty("app.remote." + integer + ".name");
            String url = config.getProperty("app.remote." + integer + ".url");
            int warningThreshold = Integer.parseInt(config.getProperty("app.remote." + integer + ".warning.threshold.minutes"));
            int interval = Integer.parseInt(config.getProperty("app.remote." + integer + ".interval"));
            return new RemoteSystem(name, url, warningThreshold, interval);
        }).toList();
    }

    public static void main(String[] args) {
        new Launcher().start();
    }

    public void start() {
        List<RemoteSystem> remoteSystems = getRemoteSystems();

        TrayHelper helper = TrayHelper.getInstance();


        for (RemoteSystem remoteSystem : remoteSystems) {
            UUID sys = helper.addStatusItem(remoteSystem.name(), TrayHelper.Status.WARNING, "No Connection");
            executor.scheduleAtFixedRate(() -> updateStatus(sys, remoteSystem, helper), 0, 30, TimeUnit.SECONDS);
        }
    }

    private void updateStatus(UUID sys, RemoteSystem remoteSystem, TrayHelper trayHelper) {
        try {

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url(remoteSystem.url())
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            String rawResponse = response.body().string();

            StatusResponse statusResponse = mapper.readValue(rawResponse, StatusResponse.class);

            LocalDateTime lastBackUpTime = LocalDateTime.parse(statusResponse.getBackup().getMinutely_last_backup(), dateTimeFormatter);
            if (lastBackUpTime.isBefore(LocalDateTime.parse(statusResponse.getBackup().getHourly_last_backup(), dateTimeFormatter))) {
                lastBackUpTime = LocalDateTime.parse(statusResponse.getBackup().getHourly_last_backup(), dateTimeFormatter);
            }
            if (lastBackUpTime.isBefore(LocalDateTime.parse(statusResponse.getBackup().getDaily_last_backup(), dateTimeFormatter))) {
                lastBackUpTime = LocalDateTime.parse(statusResponse.getBackup().getDaily_last_backup(), dateTimeFormatter);
            }
            String lastBackUp = "Last backup: " + lastBackUpTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            if (statusResponse.getStatus().equalsIgnoreCase("OK")) {
                if (lastBackUpTime.isBefore(LocalDateTime.now().minusMinutes(remoteSystem.warningThreshold()))) {
                    trayHelper.updateStatus(sys, TrayHelper.Status.WARNING, lastBackUp + " (Backup is older than 10 minutes)");
                } else {
                    trayHelper.updateStatus(sys, TrayHelper.Status.WORKING, lastBackUp);
                }
            } else {
                trayHelper.updateStatus(sys, TrayHelper.Status.WARNING, lastBackUp);
            }
        } catch (Exception e) {
            log.error(e);
            trayHelper.updateStatus(sys, TrayHelper.Status.ERROR, "No Connection");
        }
    }
    //end of main
}
