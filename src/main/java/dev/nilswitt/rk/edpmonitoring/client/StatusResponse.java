package dev.nilswitt.rk.edpmonitoring.client;

import lombok.Data;

@Data
public class StatusResponse {
    private String service;
    private String status;
    private String version;
    private BackUpStatusResponse backup;

}
