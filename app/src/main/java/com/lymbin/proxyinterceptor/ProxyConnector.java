package com.lymbin.proxyinterceptor;

import android.content.Context;

import androidx.annotation.RequiresPermission;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class ProxyConnector {
    public static String resetCommand = "iptables -t nat -F";
    public static String startCommand = "iptables -t nat -A OUTPUT -p tcp --dport %s -j DNAT --to-destination %s:%s";
    public static String checkCommand = "iptables -L -t nat | grep DNAT";

    static boolean proxyStatus = false;

    static String defaultAddress = "127.0.0.1";
    static String defaultPort = "8080";

    private static String proxyPrefsFile = "proxy.prefs";

    /**
     * Split commands and run it as su.
     *
     * @param commandStrings commands
     * @return true
     */
    static boolean startProxy(String commandStrings) {
        String[] commands = commandStrings.split(",");
        for (int i = 0; i < commands.length; i++) {
            commands[i] = commands[i].trim();
        }

        SudoWorker.sudo(commands);
        return true;
    }

    /**
     * Run reset command as su.
     */
    static void resetProxy() {
        SudoWorker.sudo(resetCommand);
    }

    /**
     * Run checkProxy command as su. Parse address and port of proxy from return.
     *
     * @return Status of NAT routing: on/off.
     */
    static boolean checkProxy() {
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
                if (proxyData.length == 2) {
                    defaultAddress = proxyData[0];
                    defaultPort = proxyData[1];
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generate commands from input values.
     *
     * @param address Proxy address from UI.
     * @param port Proxy port from UI.
     * @param destPorts Dest Ports from UI.
     * @return generated command.
     */
    static String generateCommand(final String address, final String port, final String... destPorts) {
        StringBuilder commandStr = new StringBuilder();
        for (String destPort:destPorts) {
            destPort = destPort.trim();
            commandStr.append(String.format(startCommand, destPort, address, port)).append(", ");
        }

        return commandStr.substring(0, commandStr.length()-2);
    }

    /**
     * Method using for saving proxy data
     *
     * @param c Context
     * @param address Proxy address string
     * @param port Proxy port string
     * @param destPorts Proxy destPorts string
     */
    static void saveProxyData(Context c, final String address, final String port, final String destPorts) {
        try {
            FileOutputStream fos = c.openFileOutput(proxyPrefsFile, Context.MODE_PRIVATE);
            fos.write((address + ";" + port + ";" + destPorts).getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method using for reading proxy data
     *
     * @param c Context
     * @return readed data
     */
    static String[] readProxyData(Context c) {
        try {
            StringBuilder buffer = new StringBuilder();
            FileInputStream fis = c.openFileInput(proxyPrefsFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String r;
            while ((r = reader.readLine()) != null) {
                buffer.append(r);
            }
            fis.close();
            return buffer.toString().split(";");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static void clearProxyData(Context c) {
        c.deleteFile(proxyPrefsFile);
    }
}
