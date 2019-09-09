package com.lymbin.proxyinterceptor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import java.io.DataOutputStream;

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
        if (addressText.getText().toString().isEmpty())
            addressText.setText(defaultAddress);
        if (portText.getText().toString().isEmpty())
            portText.setText(defaultPort);
        generateCommand();
        String commandStrings = commandText.getText().toString();
        String[] commands = commandStrings.split(",");
        for (int i = 0; i < commands.length; i++) {
            commands[i] = commands[i].trim();
        }

        sudo(commands);
    }

    public void onResetButtonClick(View view) {
        sudo(resetCommand);
    }

    public void generateCommand() {
        String[] destPorts = destPortText.getText().toString().split(",");
        String commandStr = "";
        for (String port:destPorts) {
            port = port.trim();
            commandStr += String.format(startCommand, port, addressText.getText().toString(), portText.getText().toString()) + ", ";
        }
        commandStr = commandStr.substring(0, commandStr.length()-2);
        commandText.setText(commandStr);
    }


    public static void sudo(String... strings) {
        try {
            Process su = Runtime.getRuntime().exec("su");

            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            for (String s : strings) {
                outputStream.writeBytes(s + "\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
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
