package com.itismob.s03.group7.leef.ui;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Tells the app whether the phone has a working internet connection, so every screen can show an
 * "You're offline" bar. Started once from {@link com.itismob.s03.group7.leef.LeefApplication}.
 */
public final class ConnectionMonitor {

    private static final MutableLiveData<Boolean> ONLINE = new MutableLiveData<>(true);

    private ConnectionMonitor() {
    }

    /** True while the phone is connected and the connection reaches the internet. */
    @NonNull
    public static LiveData<Boolean> online() {
        return ONLINE;
    }

    public static void start(@NonNull Application application) {
        ConnectivityManager manager = application.getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return;
        }
        ONLINE.setValue(hasInternet(manager));
        try {
            manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    ONLINE.postValue(hasInternet(manager));
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                                                  @NonNull NetworkCapabilities capabilities) {
                    ONLINE.postValue(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
                }

                @Override
                public void onLost(@NonNull Network network) {
                    // The default network went away; a new one would report itself through onAvailable.
                    ONLINE.postValue(false);
                }
            });
        } catch (RuntimeException ignored) {
            // Without the callback the bar simply never appears.
        }
    }

    private static boolean hasInternet(@NonNull ConnectivityManager manager) {
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
