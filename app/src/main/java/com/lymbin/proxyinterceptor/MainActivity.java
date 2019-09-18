package com.lymbin.proxyinterceptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    private EditText commandText;
    private EditText addressText;
    private EditText portText;
    private EditText destPortText;

    private MenuItem proxyIndicator;

    private NotificationManager notificationManager;
    private static final String NOTIFICATION_CHANNEL_ID = "proxyinterceptor_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        commandText = findViewById(R.id.command);
        addressText = findViewById(R.id.proxyAddress);
        portText = findViewById(R.id.proxyPort);
        destPortText = findViewById(R.id.proxyDestPort);

        addressText.setOnFocusChangeListener(new DataEditorActionListener());
        portText.setOnFocusChangeListener(new DataEditorActionListener());
        destPortText.setOnFocusChangeListener(new DataEditorActionListener());

        checkProxyStarted();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (getIntent().hasExtra("proxyStatus")) {
            boolean proxyStatus = getIntent().getBooleanExtra("proxyStatus", false);
            if (!proxyStatus) {
                resetProxy();
                RemoveNotification();
            }
        }
        else {
            initialCheck(getApplicationContext());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        proxyIndicator = menu.findItem(R.id.miIndicator);
        setProxyIndicator(ProxyConnector.proxyStatus);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.miIndicator) {
            showToast(proxyIndicator.getTitle().toString(), Gravity.BOTTOM, Toast.LENGTH_SHORT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onStartButtonClick(View view) {
        setProxyUi(ProxyConnector.defaultAddress, ProxyConnector.defaultPort);
        generateCommand();
        boolean proxyStatus = ProxyConnector.startProxy(commandText.getText().toString());
        setProxyIndicator(proxyStatus);
    }

    public void onResetButtonClick(View view) {
        resetProxy();
    }

    public void resetProxy() {
        ProxyConnector.resetProxy();
        setProxyIndicator(false);
    }

    public void generateCommand() {
        String[] destPorts = destPortText.getText().toString().split(",");
        String command = ProxyConnector.generateCommand(addressText.getText().toString(), portText.getText().toString(), destPorts);
        commandText.setText(command);
    }

    public void showToast(String message, int gravity, int length) {
        Toast toast = Toast.makeText(getApplicationContext(),
                message,
                length);
        toast.setGravity(gravity, 0, 50);
        toast.show();
    }

    private void initialCheck(Context context) {
        String wifiGateway = wifiGetGateway(context);
        if (wifiGateway == null) {
            showToast(getString(R.string.wifi_down_err_msg), Gravity.BOTTOM, Toast.LENGTH_LONG);
        }
        else {
            ProxyConnector.defaultAddress = wifiGateway;
            if (!ProxyConnector.proxyStatus) {
                setProxyUi(ProxyConnector.defaultAddress, ProxyConnector.defaultPort);
                generateCommand();
            }
        }
    }

    protected String wifiGetGateway(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);

        if (wifiManager != null) {
            int ipAddress = wifiManager.getDhcpInfo().gateway;

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            String ipAddressString;
            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e(getString(R.string.log_tag), getString(R.string.unknown_host_msg));
                ipAddressString = null;
            }
            return ipAddressString;
        }
        else {
            return null;
        }
    }

    private void checkProxyStarted() {
        String address = "";
        String port = "";
        if (ProxyConnector.checkProxy(address, port)) {
            setProxyUi(address, port);
            generateCommand();
            setProxyIndicator(true);
        }
    }

    private void setProxyUi(final String proxyAddress, final String proxyPort) {
        if (addressText.getText().toString().isEmpty())
            addressText.setText(proxyAddress);
        if (portText.getText().toString().isEmpty())
            portText.setText(proxyPort);
    }

    private void setProxyIndicator (boolean status) {
        ProxyConnector.proxyStatus = status;
        if (proxyIndicator != null) {
            if (ProxyConnector.proxyStatus) {
                proxyIndicator.getIcon().setColorFilter( getResources().getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
                proxyIndicator.setTitle("Proxy: On");
                MakeNotification(getString(R.string.log_tag), "Proxy is up on " + addressText.getText().toString() + ":" + portText.getText().toString());
            }
            else {
                proxyIndicator.getIcon().setColorFilter( getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
                proxyIndicator.setTitle("Proxy: Off");
                RemoveNotification();
            }
        }
    }

    private void MakeNotification(String title, String text) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent openAppItent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(getApplicationContext(), MainActivity.class);
        stopIntent.putExtra("proxyStatus", false);
        PendingIntent stop = PendingIntent.getActivity(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openAppItent)
                .setOngoing(true)
                .addAction(R.mipmap.ic_launcher, "Stop", stop);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void RemoveNotification() {
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    private class DataEditorActionListener implements TextView.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                generateCommand();
            }
        }
    }

}
