package com.lymbin.proxyinterceptor;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class SudoWorker {
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

    public static String sudoWithReturn(String string) {
        // TODO: make this work
        String result = "";
        try {
            ProcessBuilder builder = new ProcessBuilder("su");
            builder.redirectErrorStream(true);

            Process su = builder.start();

            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(su.getInputStream()));

            outputStream.writeBytes(string + "\n");
            outputStream.flush();
            String line;
            char[] buffer = new char[1024];
            bufferedReader.read(buffer);

            while ((line = bufferedReader.readLine()) != null)
                result += line;

            outputStream.writeBytes("exit\n");
            outputStream.flush();


            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
