package dev.nilswitt.rk.edpmonitoring.client.helpers;

import dev.nilswitt.rk.edpmonitoring.client.Launcher;
import dev.nilswitt.rk.edpmonitoring.client.exceptions.NotSupportedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrayHelper {
    private static final Logger log = LogManager.getLogger();

    private static TrayHelper instance;

    public static TrayHelper getInstance() throws NotSupportedException {
        if (instance == null) {
            instance = new TrayHelper();
        }
        return instance;
    }


    private final TrayIcon trayIcon;
    private final Map<UUID, MenuItem> statusMenuItems = new HashMap<>();
    private final Map<UUID, Status> statusItemValues = new HashMap<>();

    private TrayHelper() throws NotSupportedException {
        if (!SystemTray.isSupported()) {
            throw new NotSupportedException("System tray is not supported on this platform.");
        }
        SystemTray systemTray = SystemTray.getSystemTray();
        PopupMenu trayPopupMenu = new PopupMenu();

        MenuItem header = new MenuItem("IuK System status");
        header.setEnabled(false);
        trayPopupMenu.add(header);
        trayPopupMenu.addSeparator();

        MenuItem action = new MenuItem("Action");
        action.addActionListener(e -> JOptionPane.showMessageDialog(null, "Action Clicked"));
        trayPopupMenu.add(action);

        MenuItem close = new MenuItem("Close");
        close.addActionListener(e -> System.exit(0));
        trayPopupMenu.add(close);

        trayIcon = new TrayIcon(getImageByType(Status.WARNING), "IuK System status", trayPopupMenu);
        trayIcon.setImageAutoSize(true);

        try {
            systemTray.add(trayIcon);
        } catch (AWTException awtException) {
           log.error("Failed to add tray icon", awtException);
        }
        updateTrayIcon();
    }


    public UUID addStatusItem(String name, Status initialStatus, String message) {
        UUID uuid = UUID.randomUUID();
        MenuItem item = new MenuItem(name + ": " + message);
        statusMenuItems.put(uuid, item);
        statusItemValues.put(uuid, initialStatus);
        trayIcon.getPopupMenu().insert(item, 1);
        updateTrayIcon();
        return uuid;
    }

    public void updateStatus(UUID is, Status newStatus, String message) {
        if (!statusMenuItems.containsKey(is)) {
            throw new IllegalArgumentException("No status item found for the given UUID.");
        }
        statusItemValues.put(is, newStatus);
        MenuItem item = statusMenuItems.get(is);
        item.setLabel(item.getLabel().split(":")[0] + ": " + message);
        updateTrayIcon();
    }

    private void updateTrayIcon() {
        Status highestStatus = Status.WORKING;
        for (Status status : statusItemValues.values()) {
            if (status.ordinal() > highestStatus.ordinal()) {
                highestStatus = status;
            }
        }
        trayIcon.setImage(getImageByType(highestStatus));
    }

    public enum Status {
        WORKING,
        WARNING,
        ERROR
    }

    private static Image getImageByType(Status type) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Launcher.class.getClassLoader().getResource("working.png");
        return switch (type) {
            case WORKING -> toolkit.getImage(Launcher.class.getResource("working.png"));
            case WARNING -> toolkit.getImage(Launcher.class.getResource("warning.png"));
            case ERROR -> toolkit.getImage(Launcher.class.getResource("error.png"));
        };
    }
}
