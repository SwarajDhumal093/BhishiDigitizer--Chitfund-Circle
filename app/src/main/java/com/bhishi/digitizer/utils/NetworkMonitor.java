package com.bhishi.digitizer.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

/**
 * Lightweight application-wide connectivity monitor.
 *
 * Online means Android currently has a validated internet connection, not merely a connected
 * Wi-Fi/mobile network. This avoids telling the user they are online when the network has no
 * usable internet access.
 */
public final class NetworkMonitor {

    public interface Listener {
        void onConnectivityChanged(boolean online);
    }

    private final ConnectivityManager connectivityManager;
    private final Listener listener;
    private boolean registered;

    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            dispatchCurrentState();
        }

        @Override
        public void onLost(@NonNull Network network) {
            dispatchCurrentState();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            dispatchCurrentState();
        }
    };

    public NetworkMonitor(Context context, Listener listener) {
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (registered || connectivityManager == null) return;
        registered = true;
        connectivityManager.registerDefaultNetworkCallback(callback);
        dispatchCurrentState();
    }

    public void stop() {
        if (!registered || connectivityManager == null) return;
        registered = false;
        try {
            connectivityManager.unregisterNetworkCallback(callback);
        } catch (IllegalArgumentException ignored) {
            // Callback was already unregistered by the framework.
        }
    }

    public boolean isOnline() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void dispatchCurrentState() {
        if (listener != null) listener.onConnectivityChanged(isOnline());
    }
}
