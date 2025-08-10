package com.linkgrove.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
@Slf4j
public class GeoIpService {

    @Value("${GEOIP_ENABLED:false}")
    private boolean enabled;

    @Value("${GEOIP_DB_PATH:}")
    private String dbPath;

    private com.maxmind.geoip2.DatabaseReader reader;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (!enabled) {
            log.info("GeoIP disabled");
            return;
        }
        try {
            java.io.File database = new java.io.File(dbPath);
            if (!database.exists()) {
                log.warn("GeoIP DB not found at {}. Geo analytics will be disabled.", dbPath);
                enabled = false;
                return;
            }
            reader = new com.maxmind.geoip2.DatabaseReader.Builder(database).build();
            log.info("GeoIP database loaded: {}", dbPath);
        } catch (Exception e) {
            log.warn("Failed to initialize GeoIP reader: {}", e.getMessage());
            enabled = false;
        }
    }

    public String resolveCountryIso2(String ip) {
        if (!enabled || ip == null || ip.isBlank() || reader == null) return null;
        try {
            InetAddress address = InetAddress.getByName(ip);
            var response = reader.country(address);
            String iso = response.getCountry().getIsoCode();
            if (iso == null || iso.isBlank()) return null;
            return iso;
        } catch (Exception e) {
            return null;
        }
    }
}


