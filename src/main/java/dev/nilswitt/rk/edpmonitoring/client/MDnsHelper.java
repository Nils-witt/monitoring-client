package dev.nilswitt.rk.edpmonitoring.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

public class MDnsHelper {

    private static final Logger log = LoggerFactory.getLogger(MDnsHelper.class);
    JmDNS jmDNS;

    public MDnsHelper() {

    }

    public void start() {
        try {
            this.jmDNS = JmDNS.create(InetAddress.getLocalHost());

            this.jmDNS.addServiceListener("_iuk._tcp.local.", new SampleListener());
            ServiceInfo[] serviceInfos = this.jmDNS.list("_iuk._tcp.local.");
            for (ServiceInfo info : serviceInfos) {
                System.out.println("Service found: " + info);
            }

            System.out.println("Listening for services...");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void stop() {
        if (jmDNS != null) {
            try {
                jmDNS.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }



    private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }
}
