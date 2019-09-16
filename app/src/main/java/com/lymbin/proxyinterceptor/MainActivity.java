package com.lymbin.proxyinterceptor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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
                proxyIndicator.setTitle("Proxy: On");
            }
            else {
                proxyIndicator.setTitle("Proxy: Off");
            }
        }
        else {
            proxyStatus = status;
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
