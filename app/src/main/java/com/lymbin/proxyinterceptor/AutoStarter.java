package com.lymbin.proxyinterceptor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AutoStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            String[] data = ProxyConnector.readProxyData(context);
            if (data != null && data.length == 3) {
                String[] destPorts = data[2].split(",");
                String command = ProxyConnector.generateCommand(data[0], data[1], destPorts);
                ProxyConnector.startProxy(command);
            }
        }
    }
}
