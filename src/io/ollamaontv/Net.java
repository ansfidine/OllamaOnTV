package io.ollamaontv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.UnknownHostException;

final class Net {
    private Net() {}

    static boolean vpn(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = manager.getActiveNetwork();
            NetworkCapabilities caps = manager.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } catch (Exception e) {
            return false;
        }
    }

    static boolean local(String endpoint) {
        String value = endpoint == null ? "" : endpoint.toLowerCase();
        return value.contains("127.0.0.1") || value.contains("localhost") || value.contains("192.168.")
                || value.contains("10.") || value.contains(".local") || value.contains("172.16.")
                || value.contains("172.17.") || value.contains("172.18.") || value.contains("172.19.")
                || value.contains("172.2") || value.contains("172.30.") || value.contains("172.31.");
    }

    static String friendly(Context context, String endpoint, String error) {
        String detail = error == null ? "" : error;
        boolean cloud = endpoint != null && endpoint.toLowerCase().contains("ollama.com");
        if (cloud) {
            if (vpn(context)) {
                return "Couldn't reach Ollama Cloud. Your VPN is on. Switch to another VPN server, or turn the VPN off for OllamaOnTV, then retry.\n\n" + detail;
            }
            return "Couldn't reach Ollama Cloud. Check the internet connection, then retry.\n\n" + detail;
        }
        if (local(endpoint)) {
            return "Couldn't reach the local relay or Ollama server. Start the relay on your Mac and run tools/connect.sh, or switch to the cloud endpoint in Settings.\n\n" + detail;
        }
        return "Couldn't reach the endpoint. Check the address and that the server is running, then retry.\n\n" + detail;
    }
}
