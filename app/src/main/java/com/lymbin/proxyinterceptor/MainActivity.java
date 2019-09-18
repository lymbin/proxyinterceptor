package com.lymbin.proxyinterceptor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static String defaultAddress = "127.0.0.1";
    public static String defaultPort = "8080";
    public static String resetCommand = "iptables -t nat -F";
    public static String startCommand = "iptables -t nat -A OUTPUT -p tcp --dport %s -j DNAT --to-destination %s:%s";

    private EditText commandText;
    private EditText addressText;
    private EditText portText;
    private EditText destPortText;

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
    }

    public void onResetButtonClick(View view) {
        SudoWorker.sudo(resetCommand);
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

    private void setProxyUi(final String proxyAddress, final String proxyPort) {
        if (addressText.getText().toString().isEmpty())
            addressText.setText(proxyAddress);
        if (portText.getText().toString().isEmpty())
            portText.setText(proxyPort);
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
