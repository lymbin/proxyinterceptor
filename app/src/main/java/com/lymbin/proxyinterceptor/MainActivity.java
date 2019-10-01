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

    /**
     * Creates main activity. Gets ui widgets by findViewById method and set OnFocusChangeListener for EditText widgets.
     * Also here a some proxy checks: first check is proxy started check, second is wifi connection check.
     * Notification button 'Stop' opens MainActivity with extra 'proxyStatus = false'.
     * In this case app should reset proxy and remove notifications.
     *
     * @param savedInstanceState default
     */
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

    /**
     * This method called when Menu is shown on app screen.
     * When it's happens method read proxy status and set indicator of it.
     *
     * @param menu Menu for this activity
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        proxyIndicator = menu.findItem(R.id.miIndicator);
        setProxyIndicator(ProxyConnector.proxyStatus);
        return true;
    }

    /**
     * This method called when MenuItem is selected.
     * If selected an indicator app show the toast message.
     *
     * @param item Selected menu item
     * @return true or super.onOptionsItemSelected(item) if it's not an indicator
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.miIndicator) {
            showToast(proxyIndicator.getTitle().toString(), Gravity.BOTTOM, Toast.LENGTH_SHORT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * On Start button click handler.
     * Generates command and start proxy with that command.
     * Set proxy indicator.
     *
     * @param view not used
     */
    public void onStartButtonClick(View view) {
        setProxyUi(ProxyConnector.defaultAddress, ProxyConnector.defaultPort);
        generateCommand();
        boolean proxyStatus = ProxyConnector.startProxy(commandText.getText().toString());
        setProxyIndicator(proxyStatus);
    }

    /**
     * On Reset button click handler.
     * Resets proxy.
     *
     * @param view not used
     */

    public void onResetButtonClick(View view) {
        resetProxy();
    }

    /**
     * Call resetProxy and set proxy indicator to false.
     *
     */
    public void resetProxy() {
        ProxyConnector.resetProxy();
        setProxyIndicator(false);
    }

    /**
     * Parse data from UI fields and send it to command generator.
     */
    public void generateCommand() {
        String[] destPorts = destPortText.getText().toString().split(",");
        String command = ProxyConnector.generateCommand(addressText.getText().toString(), portText.getText().toString(), destPorts);
        commandText.setText(command);
    }

    /**
     * Show message as toast notification.
     *
     * @param message Message to show
     * @param gravity Where show the message
     * @param length Length of toast
     */
    public void showToast(String message, int gravity, int length) {
        Toast toast = Toast.makeText(getApplicationContext(),
                message,
                length);
        toast.setGravity(gravity, 0, 50);
        toast.show();
    }

    /**
     * Initial check for wifi connection. It's also set wifi gateway as proxy address in UI field.
     *
     * @param context App context.
     */
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

    /**
     * Get wifi gateway via WifiManager.getDhcpInfo().gateway.
     *
     * @param context App context.
     * @return gateway address or null in case of errors
     */
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

    /**
     * Checks proxy starts already. In case it's started - sets UI params and generate command.
     */
    private void checkProxyStarted() {
        String address = "";
        String port = "";
        if (ProxyConnector.checkProxy()) {
            setProxyUi(ProxyConnector.defaultAddress, ProxyConnector.defaultPort);
            generateCommand();
            setProxyIndicator(true);
        }
    }

    /**
     * Set address and port of proxy in UI fields.
     *
     * @param proxyAddress Address of proxy server
     * @param proxyPort Port of proxy server
     */
    private void setProxyUi(final String proxyAddress, final String proxyPort) {
        if (addressText.getText().toString().isEmpty())
            addressText.setText(proxyAddress);
        if (portText.getText().toString().isEmpty())
            portText.setText(proxyPort);
    }

    /**
     * Set proxy status in proxy indicator menu item (green or red icon) and make/remove notification.
     *
     * @param status Proxy Up/Down status.
     */
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

    /**
     * Make app notification with title and text via notificationManager.
     *
     * @param title Title of notification.
     * @param text Text of notification.
     */
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

    /**
     * Method to remove all app notifications.
     */
    private void RemoveNotification() {
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    private class DataEditorActionListener implements TextView.OnFocusChangeListener {
        /**
         * Method called when edittext focus has changed.
         *
         * @param v not used
         * @param hasFocus status of edittext focus
         */
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                generateCommand();
            }
        }
    }

}
