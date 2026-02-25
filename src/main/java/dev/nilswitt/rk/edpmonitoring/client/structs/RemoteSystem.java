package dev.nilswitt.rk.edpmonitoring.client.structs;

public record RemoteSystem(String name, String url, int warningThreshold, int interval) {
}
