package edu.cmu.sei.ttg.aaiot.as;

import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import se.sics.ace.AceException;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Controller {
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

        pairWithRS(authorizationServer);

        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("");
            System.out.println("Choose (p)air, (q)uit, or do nothing to keep server running: ");
            char choice = scanner.next().charAt(0);

            switch (choice) {
                case 'p':
                    pair();
                    System.out.println("Paired!");
                    break;
                case 'q':
                    System.exit(0);
                default:
                    System.out.println("Invalid command.");
            }
        }
    }

    private void pair() throws Exception
    {
        System.out.println("Started pairing");
        PairingManager pairingManager = new PairingManager(authorizationServer);
        pairingManager.pairClient(asId, InetAddress.getByName("localhost"), 9876);
        System.out.println("Finished pairing");
    }

    private void pairWithRS(AuthorizationServer as) throws COSE.CoseException, AceException
    {
        // TODO: This should be called as the result of pairing RS with AS.
        String rsName = "rs1";
        byte[] rsSharedKey256Bytes = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
        OneKey rsPSK = createOneKeyFromBytes(rsSharedKey256Bytes);
        Set<String> scopes = new HashSet<>();
        scopes.add("r_temp");

        as.addResourceServer(rsName, rsPSK, scopes);
    }

    private void pairWithClient(AuthorizationServer as) throws COSE.CoseException, AceException
    {
        // TODO: This should be called as the result of pairing Client with AS.
        String clientName = "clientA";
        byte[] clientAsharedKey256Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
        OneKey clientAPSK = createOneKeyFromBytes(clientAsharedKey256Bytes);

        as.addClient(clientName, clientAPSK);
    }

    private OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        OneKey key = new OneKey(keyData);
        return key;
    }

}
