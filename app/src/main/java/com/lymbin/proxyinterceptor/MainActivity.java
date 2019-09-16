package com.lymbin.proxyinterceptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static String defaultAddress = "127.0.0.1";
    public static String defaultPort = "8080";
    public static String resetCommand = "iptables -t nat -F";
    public static String startCommand = "iptables -t nat -A OUTPUT -p tcp --dport %s -j DNAT --to-destination %s:%s";
    public static String checkCommand = "iptables -L -t nat | grep DNAT";

    private EditText commandText;
    private EditText addressText;
    private EditText portText;
    private EditText destPortText;

    private MenuItem proxyIndicator;
    private boolean proxyStatus = false;

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

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (getIntent().hasExtra("proxyStatus")) {
            boolean proxyStatus = getIntent().getBooleanExtra("proxyStatus", false);
            if (!proxyStatus) {
                resetProxy();
                RemoveNotification();
            }
        }

        checkProxyStarted();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        proxyIndicator = menu.findItem(R.id.miIndicator);
        setProxyIndicator(proxyStatus);
        return true;
    }

    public void onStartButtonClick(View view) {
        setProxyUi(defaultAddress, defaultPort);
        generateCommand();
        String commandStrings = commandText.getText().toString();
        String[] commands = commandStrings.split(",");
        for (int i = 0; i < commands.length; i++) {
            commands[i] = commands[i].trim();
        }

        SudoWorker.sudo(commands);
        setProxyIndicator(true);
    }

    public void onResetButtonClick(View view) {
        resetProxy();
    }

    public void resetProxy() {
        SudoWorker.sudo(resetCommand);
        setProxyIndicator(false);
    }

    public void generateCommand() {
        String[] destPorts = destPortText.getText().toString().split(",");
        StringBuilder commandStr = new StringBuilder();
        for (String port:destPorts) {
            port = port.trim();
            commandStr.append(String.format(startCommand, port, addressText.getText().toString(), portText.getText().toString())).append(", ");
        }
        commandText.setText(commandStr.substring(0, commandStr.length()-2));
    }

    private void checkProxyStarted() {
        String checkResult = SudoWorker.sudoWithReturn(checkCommand);
        String[] split = checkResult.split("tcp dpt:http to:");
        try {
            if (split.length == 2) {
                String proxyInfo = split[1];
                int dnatIndex = proxyInfo.indexOf("DNAT");
                String[] proxyData = null;
                if (dnatIndex == -1) {
                    proxyData = proxyInfo.split(":");
                }
                else {
                    proxyData = proxyInfo.substring(0, dnatIndex).split(":");
                }
                setProxyUi(proxyData[0], proxyData[1]);

                generateCommand();
                setProxyIndicator(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setProxyUi(final String proxyAddress, final String proxyPort) {
        if (addressText.getText().toString().isEmpty())
            addressText.setText(proxyAddress);
        if (portText.getText().toString().isEmpty())
            portText.setText(proxyPort);
    }

    private void setProxyIndicator (boolean status) {
        if (proxyIndicator != null) {
            if (status) {
                proxyIndicator.getIcon().setColorFilter( getResources().getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP);
                proxyIndicator.setTitle("Proxy: On");
                MakeNotification("ProxyInterceptor", "Proxy is up on " + addressText.getText().toString() + ":" + portText.getText().toString());
            }
            else {
                proxyIndicator.getIcon().setColorFilter( getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
                proxyIndicator.setTitle("Proxy: Off");
                RemoveNotification();
            }
        }
        else {
            proxyStatus = status;
        }
    }

    private void MakeNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopIntent = new Intent(getApplicationContext(), MainActivity.class);
        stopIntent.putExtra("proxyStatus", false);
        PendingIntent stop = PendingIntent.getActivity(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
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
