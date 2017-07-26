package edu.cmu.sei.ttg.aaiot.as;

import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;

import java.net.InetAddress;
import java.util.Scanner;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Controller {
    private static final int CLIENT_PAIRING_PORT = 9876;
    private static final int DEVICE_PAIRING_PORT = 9877;

    private static final String CLIENT_IP = "localhost";
    private static final String DEVICE_IP = "localhost";

    private AuthorizationServer authorizationServer;
    private String asId = "AAIoT_AS";


    public void run() throws Exception
    {
        // TODO: load from somewhere.
        String rootPassword = "";

        // Create the server and its DB.
        authorizationServer = new AuthorizationServer(asId);
        authorizationServer.createDB(rootPassword);
        authorizationServer.connectToDB();

        // Start the server.
        authorizationServer.start();

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (c) pair client, (d) pair device, (q)uit, or do nothing to keep server running: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'c':
                    pair(CLIENT_IP, CLIENT_PAIRING_PORT);
                    System.out.println("Paired!");
                    break;
                case 'd':
                    pair(DEVICE_IP, DEVICE_PAIRING_PORT);
                    System.out.println("Paired!");
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
        pairingManager.pairClient(asId, InetAddress.getByName(server), port);
        System.out.println("Finished pairing");
    }
}
