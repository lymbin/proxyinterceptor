package com.lymbin.proxyinterceptor;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class SudoWorker {
    /**
     * Run commands as su.
     *
     * @param strings command that run in su process.
     */
    static void sudo(String... strings) {
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

    /**
     * Run command as su and return it result.
     *
     * @param string command.
     * @return return from command.
     */
    static String sudoWithReturn(String string) {
        try {
            Process su = Runtime.getRuntime().exec(new String[]{"su", "-c", string});
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(su.getInputStream()));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null)
                result.append(line);

            outputStream.close();

            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
