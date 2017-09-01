package edu.cmu.sei.ttg.aaiot.as;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.NotFoundException;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import edu.cmu.sei.ttg.aaiot.config.Config;
import se.sics.ace.AceException;

import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Controller
{
    private static final byte[] CLIENT_PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    // We do not need to know this, we just have it hear to simplify tests.
    private static final byte[] PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final String CONFIG_FILE = "config.json";

    private static final String DEFAULT_CLIENT_IP = "localhost";
    private static final String DEFAULT_DEVICE_IP = "localhost";

    private static final String QR_CODE_IMAGE_FILE_PATH = "qrcode.png";
    private static final String QR_CODE_TEST_IMAGE_FILE_PATH = "qrcode_original.png";

    private AuthorizationServer authorizationServer;

    public void run() throws Exception
    {
        Config.load(CONFIG_FILE);

        String rootPassword = Config.data.get("root_db_pwd");

        //generateQRCode();

        // Create the server and its DB.
        String asId = Config.data.get("id");
        authorizationServer = new AuthorizationServer(asId);
        authorizationServer.createDB(rootPassword);
        authorizationServer.connectToDB(Config.data.get("acl_path"));

        // Start the server.
        authorizationServer.start();

        Scanner scanner = new Scanner(System.in);

        while(true) {
            try
            {
                System.out.println("");
                System.out.println("Server running ");
                System.out.println("(c) pair client");
                System.out.println("(d) pair device");
                System.out.println("(h) handle rules");
                System.out.println("(l) list paired clients and devices");
                System.out.println("(r) remove a paired client");
                System.out.println("(e) remove a paired device");
                System.out.println("(v) revoke an existing token");
                System.out.println("(q)uit");
                char choice = scanner.nextLine().charAt(0);

                switch (choice)
                {
                    case 'c':
                        System.out.println("");
                        System.out.println("Input client's IP, or (Enter) to use default (" + DEFAULT_CLIENT_IP + "): ");
                        String ip = scanner.nextLine();
                        if (ip.equals(""))
                        {
                            ip = DEFAULT_CLIENT_IP;
                        }

                        pair(ip, CLIENT_PAIRING_KEY);
                        System.out.println("Finished pairing procedure!");
                        break;
                    case 'd':
                        byte[] psk = getPairingPSK();
                        if(psk == null)
                        {
                            System.out.println("Could not obtain pairing key, aborting procedure.");
                            break;
                        }

                        System.out.println("");
                        System.out.println("Input devices's IP, or (Enter) to use default (" + DEFAULT_DEVICE_IP + "): ");
                        String device_ip = scanner.nextLine();
                        if (device_ip.equals(""))
                        {
                            device_ip = DEFAULT_DEVICE_IP;
                        }

                        pair(device_ip, psk);
                        System.out.println("Finished pairing procedure!");
                        break;
                    case 'h':
                        manageRules();
                        break;
                    case 'l':
                        listPairedClientsAndDevices();
                        break;
                    case 'r':
                        System.out.println("");
                        System.out.println("Input client's name/id ");
                        String clientName = scanner.nextLine();
                        removePairedClient(clientName);
                        break;
                    case 'e':
                        System.out.println("");
                        System.out.println("Input RS's name/id ");
                        String rsName = scanner.nextLine();
                        removePairedRS(rsName);
                        break;
                    case 'v':
                        revokeToken();
                        break;
                    case 'q':
                        authorizationServer.stop();
                        System.exit(0);
                    default:
                        System.out.println("Invalid command.");
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error processing command: " + ex.toString());
                ex.printStackTrace();
            }
        }
    }

    // Only used to generate the QR code image to print. Should actually be in RS...
    private void generateQRCode() throws IOException
    {
        String psk = Base64.getEncoder().encodeToString(PAIRING_KEY);
        System.out.println("Base64 encoded key: " + psk);
        QRCodeManager.createQRCodeFile(psk, QR_CODE_TEST_IMAGE_FILE_PATH);
    }

    private byte[] getPairingPSK() throws IOException, InterruptedException
    {
        // Obtain QRCode image from webcam or other source.
        byte[] pskBytes = null;
        Scanner scanner = new Scanner(System.in);
        System.out.println("");
        System.out.println("Press Enter when ready to scan QR Code (or (s) to skip): ");
        String skip = scanner.nextLine();
        if(skip.equals("s"))
        {
            pskBytes = PAIRING_KEY;
        }
        else
        {
            int attemptsLeft = 5;
            while(attemptsLeft > 0)
            {
                attemptsLeft--;
                System.out.println("Getting image... ");
                Webcam webcam = Webcam.getDefault();
                webcam.open();
                ImageIO.write(webcam.getImage(), "JPG", new File(QR_CODE_IMAGE_FILE_PATH));
                webcam.close();
                System.out.println("Image obtained.");

                try
                {
                    String devicePSK = QRCodeManager.readQRCode(QR_CODE_IMAGE_FILE_PATH);
                    System.out.println("QR code decoded: " + devicePSK);
                    pskBytes = Base64.getDecoder().decode(devicePSK);
                }
                catch(NotFoundException ex)
                {
                    System.out.println("QR code not detected.");
                }

                if(attemptsLeft > 0)
                {
                    System.out.println("Ensure QR Code is well positioned. Will try again in 3 seconds. ");
                    Thread.sleep(3000);
                }
                else
                {
                    System.out.println("QR code could not be found after several attempts.");
                    break;
                }
            }
        }

        return pskBytes;
    }

    private void pair(String server, byte[] psk) throws Exception
    {
        System.out.println("Started pairing");
        PairingManager pairingManager = new PairingManager(authorizationServer);
        String asId = Config.data.get("id");
        pairingManager.pair(asId, psk, server);
        System.out.println("Finished pairing");
    }

    private void manageRules() throws IOException, AceException
    {
        Scanner scanner = new Scanner(System.in);

        boolean validClientSelected = false;
        String selectedClient = "";
        while(!validClientSelected)
        {
            System.out.println("");
            System.out.println("Input the name of the client that you want to manage rules for. The following are the valid paired clients: ");
            for (String client : authorizationServer.getClients())
            {
                System.out.println(client);
            }
            selectedClient = scanner.nextLine();

            if(authorizationServer.getClients().contains(selectedClient))
            {
                validClientSelected = true;
            }
            else
            {
                System.out.println("Invalid client.");
            }
        }

        while(true)
        {
            System.out.println("");
            System.out.println("Input the 'add' or 'remove' followed by the resource server name and scope (i.e., add rs1 read). The following are the current rules: ");
            Map<String, Set<String>> currentClientRules = authorizationServer.getRules(selectedClient);
            if(currentClientRules != null)
            {
                for (String rs : currentClientRules.keySet())
                {
                    System.out.print("RS " + rs + ": ");
                    for (String scope : authorizationServer.getRules(selectedClient).get(rs))
                    {
                        System.out.println(scope + " ");
                    }
                }
            }

            String command = scanner.nextLine();
            String[] parts = command.split(" ");
            if (parts[0].equals("add"))
            {
                authorizationServer.addRule(selectedClient, parts[1], parts[2]);
                break;
            }
            else if (parts[0].equals("remove"))
            {
                authorizationServer.removeRule(selectedClient, parts[1], parts[2]);
                break;
            }
            else
            {
                System.out.println("Invalid command.");
                break;
            }
        }
    }

    public void listPairedClientsAndDevices() throws AceException
    {
        System.out.println("");
        System.out.println("Paired clients: ");
        for(String clientName : authorizationServer.getClients())
        {
            System.out.println("  " + clientName);
        }

        System.out.println("");
        System.out.println("Paired devices: ");
        for(String resourceServer : authorizationServer.getResourceServers())
        {
            System.out.println("  " + resourceServer);
        }
    }

    public void removePairedClient(String clientName) throws AceException, IOException
    {
        authorizationServer.removeClient(clientName);
    }

    public void removePairedRS(String rsName) throws AceException, IOException
    {
        authorizationServer.removeResourceServer(rsName);
    }

    public void revokeToken() throws AceException
    {
        Map<String, Set<String>> tokensByResourceServer = authorizationServer.getAllTokensByRS();

        // List all valid tokens by resource server for the user to choose.
        System.out.println("");
        System.out.println("Valid tokens: ");
        for(String rsName : tokensByResourceServer.keySet())
        {
            for(String tokenId : tokensByResourceServer.get(rsName))
            {
                System.out.println("Token id: " + tokenId + " - for RS " + rsName + " to client " + authorizationServer.getClientForCti(tokenId));
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("");
        System.out.println("Input the id of the token to be revoked, or (q) to cancel: ");
        String tokenToRevoke = scanner.nextLine();

        if(tokenToRevoke.equals("q"))
        {
            System.out.println("Not revoking tokens.");
            return;
        }

        try
        {
            authorizationServer.revokeToken(tokenToRevoke);
            System.out.println("Token revoked");
        }
        catch(Exception ex)
        {
            System.out.println("Error revoking token: " + ex.toString());
        }
    }
}
