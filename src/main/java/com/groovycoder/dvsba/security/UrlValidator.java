package com.groovycoder.dvsba.security;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

@Component
public class UrlValidator {

    public boolean isSafe(String urlString) {
        try {
            URL url = new URL(urlString);

            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }

            InetAddress address = InetAddress.getByName(url.getHost());

            if (address.isLoopbackAddress()
                    || address.isAnyLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isMulticastAddress()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}