/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.as.commandline;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.NotFoundException;
import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import edu.cmu.sei.ttg.aaiot.config.Config;
import se.sics.ace.AceException;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by sebastianecheverria on 11/1/17.
 */
public class CommandLineUI
{
    private AuthorizationServer authorizationServer;

    public void run(AuthorizationServer authorizationServer) throws Exception
    {
        this.authorizationServer = authorizationServer;
        showMainMenu();
    }

    /**
     * Shows the main menu and calls corresponding handlers.
     */
    private void showMainMenu()
    {
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
                        System.out.println("Input client's IP, or (Enter) to use default (" + Application.DEFAULT_CLIENT_IP + "): ");
                        String ip = scanner.nextLine();
                        if (ip.equals(""))
                        {
                            ip = Application.DEFAULT_CLIENT_IP;
                        }

                        pair(ip, Application.CLIENT_PAIRING_KEY);

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
                        System.out.println("Input devices's IP, or (Enter) to use default (" + Application.DEFAULT_DEVICE_IP + "): ");
                        String device_ip = scanner.nextLine();
                        if (device_ip.equals(""))
                        {
                            device_ip = Application.DEFAULT_DEVICE_IP;
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
                        removePairedClient(scanner);
                        break;
                    case 'e':
                        removePairedRS(scanner);
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
                //ex.printStackTrace();
            }
        }
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
            pskBytes = Application.DEFAULT_DEVICE_PAIRING_KEY;
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
                ImageIO.write(webcam.getImage(), "JPG", new File(Application.QR_CODE_IMAGE_FILE_PATH));
                webcam.close();
                System.out.println("Image obtained.");

                try
                {
                    String devicePSK = QRCodeManager.readQRCode(Application.QR_CODE_IMAGE_FILE_PATH);
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
        String asId = Config.data.get("id");

        System.out.println("Started pairing");

        PairingManager pairingManager = new PairingManager(authorizationServer);
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
                return;
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
        Set<String> clients = authorizationServer.getClients();
        if(clients != null)
        {
            for (String clientName : clients)
            {
                System.out.println("  " + clientName);
            }
        }

        System.out.println("");
        System.out.println("Paired devices: ");
        Set<String> devices = authorizationServer.getResourceServers();
        if(devices != null)
        {
            for (String resourceServer : devices)
            {
                System.out.println("  " + resourceServer);
            }
        }
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

    public void removePairedClient(Scanner scanner) throws AceException, IOException
    {
        System.out.println("");
        System.out.println("Input client's name/id ");
        String clientName = scanner.nextLine();
        authorizationServer.removeClient(clientName);
    }

    public void removePairedRS(Scanner scanner) throws AceException, IOException
    {
        System.out.println("");
        System.out.println("Input RS's name/id ");
        String rsName = scanner.nextLine();
        authorizationServer.removeResourceServer(rsName);
    }

}
