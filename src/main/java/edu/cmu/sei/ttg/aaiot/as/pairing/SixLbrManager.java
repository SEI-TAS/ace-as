package edu.cmu.sei.ttg.aaiot.as.pairing;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by sebastianecheverria on 8/16/17.
 */
public class SixLbrManager
{
    // Generic input gobbler.
    private static class StreamGobbler implements Runnable
    {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer)
        {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run()
        {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }

    public static void start6lbr() throws IOException
    {
        System.out.println("Starting 6lbr service...");

        // Call service process through command line
        // TODO: check sudo requirement...
        executeProcess("service 6lbr start");

        System.out.println("6lbr service started.");
    }

    public static void stop6lbr() throws IOException
    {
        System.out.println("Stopping 6lbr service...");

        // Call service process through command line.
        // TODO: Check sudo requirement...
        executeProcess("service 6lbr stop");

        System.out.println("6lbr service stopped.");
    }

    public static void configureKey(String key)
    {
        // TODO: write key into config.
    }

    private static boolean executeProcess(String commandLine) throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", commandLine);
        builder.directory(new File(System.getProperty("user.home")));
        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        int exitCode = 0;
        try
        {
            exitCode = process.waitFor();
        }
        catch (InterruptedException e)
        {
            throw new IOException(e.toString());
        }

        return exitCode == 0;
    }
}
