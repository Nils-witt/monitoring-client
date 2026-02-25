package dev.nilswitt.rk.edpmonitoring.client;


import lombok.Data;

@Data
public class BackUpStatusResponse {
    private String hourly_last_backup;
    private String daily_last_backup;
    private String minutely_last_backup;
}
