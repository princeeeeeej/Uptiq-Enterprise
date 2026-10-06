package com.uptiq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SSLCertChecker {

    private static final Logger log = LoggerFactory.getLogger(SSLCertChecker.class);

    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    public SSLCertInfo check(String urlString) {

        try {
            URI uri = URI.create(urlString);

            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 443 : uri.getPort();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            try (SSLSocket socket = (SSLSocket) factory.createSocket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(5000);
                socket.startHandshake();

                X509Certificate[] certs =
                        (X509Certificate[]) socket.getSession().getPeerCertificates();

                if (certs.length == 0) {
                    return null;
                }

                X509Certificate leaf = certs[0];

                Instant validFrom = leaf.getNotBefore().toInstant();
                Instant validUntil = leaf.getNotAfter().toInstant();

                long daysRemaining = Math.max(
                        0,
                        ChronoUnit.DAYS.between(Instant.now(), validUntil)
                );

                String issuer = extractCN(leaf.getIssuerX500Principal().getName());

                return new SSLCertInfo(
                        issuer,
                        validFrom,
                        validUntil,
                        (int) daysRemaining
                );
            }

        } catch (Exception e) {
            log.warn("SSL check failed for {} — {}", urlString, e.getMessage());
            return null;
        }
    }

    private String extractCN(String dn) {
        Matcher matcher = CN_PATTERN.matcher(dn);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    public record SSLCertInfo(
            String issuer,
            Instant validFrom,
            Instant validUntil,
            int daysRemaining
    ) {
    }
}