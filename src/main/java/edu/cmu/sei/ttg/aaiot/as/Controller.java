package edu.cmu.sei.ttg.aaiot.as;

import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;

import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Controller {
    private static final String CONFIG_FILE = "config.json";
    private static final int CLIENT_PAIRING_PORT = 9876;
    private static final int DEVICE_PAIRING_PORT = 9877;

    private static final String DEFAULT_CLIENT_IP = "localhost";
    private static final String DEFAULT_DEVICE_IP = "localhost";

    private AuthorizationServer authorizationServer;

    public void run() throws Exception
    {
        Config.load(CONFIG_FILE);

        String rootPassword = Config.data.get("root_db_pwd");

        // Create the server and its DB.
        String asId = Config.data.get("id");
        authorizationServer = new AuthorizationServer(asId);
        authorizationServer.createDB(rootPassword);
        authorizationServer.connectToDB(Config.data.get("acl_path"));

        // Start the server.
        authorizationServer.start();

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (c) pair client, (d) pair device, (q)uit, or do nothing to keep server running: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'c':
                    System.out.println("");
                    System.out.println("Input client's IP, or (d) to use default (" + DEFAULT_CLIENT_IP + "): ");
                    String ip = scanner.next();
                    if(ip.equals("d"))
                    {
                        ip = DEFAULT_CLIENT_IP;
                    }

                    pair(ip, CLIENT_PAIRING_PORT);
                    System.out.println("Finished pairing procedure!");
                    break;
                case 'd':
                    System.out.println("");
                    System.out.println("Input devices's IP, or (d) to use default (" + DEFAULT_DEVICE_IP + "): ");
                    String device_ip = scanner.next();
                    if(device_ip.equals("d"))
                    {
                        device_ip = DEFAULT_DEVICE_IP;
                    }

                    pair(device_ip, DEVICE_PAIRING_PORT);
                    System.out.println("Finished pairing procedure!");
                    break;
                case 'q':
                    System.exit(0);
                default:
                    System.out.println("Invalid command.");
            }
        }
    }

    private void pair(String server, int port) throws Exception
    {
        System.out.println("Started pairing");
        PairingManager pairingManager = new PairingManager(authorizationServer);
        String asId = Config.data.get("id");
        pairingManager.pairClient(asId, InetAddress.getByName(server), port);
        System.out.println("Finished pairing");
    }
}
