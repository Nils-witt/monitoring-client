module dev.nilswitt.rk.edpmonitoring.client {
    requires java.desktop;
    requires javax.jmdns;
    requires org.slf4j;
    requires okhttp3;
    requires tools.jackson.databind;
    requires static lombok;
    requires org.apache.logging.log4j;


    exports dev.nilswitt.rk.edpmonitoring.client;
    exports dev.nilswitt.rk.edpmonitoring.client.exceptions;
    exports dev.nilswitt.rk.edpmonitoring.client.helpers;
    exports dev.nilswitt.rk.edpmonitoring.client.structs;
}