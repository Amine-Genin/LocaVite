package ma.locavite.payment.cmi.service;

import jakarta.servlet.http.HttpServletRequest;
import ma.locavite.payment.cmi.config.CmiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Service
public class CmiIpWhitelistService {

    private static final Logger log = LoggerFactory.getLogger(CmiIpWhitelistService.class);

    private final CmiProperties cmiProperties;

    public CmiIpWhitelistService(CmiProperties cmiProperties) {
        this.cmiProperties = cmiProperties;
    }

    public boolean isIpAllowed(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return isIpAllowed(clientIp);
    }

    public boolean isIpAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        String cleanIp = ip.trim();
        List<String> whitelist = cmiProperties.getIpWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            log.warn("Aucune IP en liste blanche CMI configurée, accès refusé par défaut");
            return false;
        }

        for (String entry : whitelist) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String pattern = entry.trim();
            if (pattern.contains("/")) {
                if (matchesCidr(cleanIp, pattern)) {
                    return true;
                }
            } else {
                if (pattern.equalsIgnoreCase(cleanIp)) {
                    return true;
                }
                if (isLoopback(cleanIp) && isLoopback(pattern)) {
                    return true;
                }
            }
        }

        log.warn("IP non autorisée pour le webhook CMI : {}", cleanIp);
        return false;
    }

    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] parts = xForwardedFor.split(",");
            return parts[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr.trim() : "unknown";
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || "localhost".equalsIgnoreCase(ip);
    }

    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress target = InetAddress.getByName(ip);
            InetAddress network = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] targetBytes = target.getAddress();
            byte[] networkBytes = network.getAddress();

            if (targetBytes.length != networkBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (targetBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0 && fullBytes < targetBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                return (targetBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.debug("Impossible d'évaluer le CIDR {} pour l'IP {}: {}", cidr, ip, e.getMessage());
            return false;
        }
    }
}
