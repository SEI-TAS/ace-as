package edu.cmu.sei.ttg.aaiot.as.pairing;

import edu.cmu.sei.ttg.aaiot.network.UDPClient;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class PairingManager
{
    private static final String separator = ":";

    private SecureRandom random = new SecureRandom();
    private ICredentialsStore credentialsStore;

    public PairingManager(ICredentialsStore credentialsStore)
    {
        this.credentialsStore = credentialsStore;
    }

    public void pairClient(String asID, InetAddress deviceIp, int devicePort) throws Exception
    {
        UDPClient udpClient = new UDPClient(deviceIp, devicePort);

        // Generate a new, random AES-256 key.
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        String psk =  Base64.getEncoder().encodeToString(key.getEncoded());

        // Send our ID and the PSK to use with us.
        System.out.println("Sending pair request");
        String pairingRequest = "p" + separator + asID + separator + psk;
        udpClient.sendData(pairingRequest);

        // Wait for reply. Format: a:c:<id> or a:i:<id>:<scope1>;<scope2>;...;<scopen>
        System.out.println("Waiting for pair reply at ");

        String reply;
        try
        {
            reply = udpClient.receiveData();
        }
        catch(SocketTimeoutException ex)
        {
            System.out.println("Cancelling pairing, wait timeout exceeded.");
            return;
        }

        String[] parts = reply.split(separator);
        if(!parts[0].equals("a"))
        {
            System.out.println("Incorrect reply received");
            return;
        }

        System.out.println("Received reply: " + reply);
        if(parts[1].equals("c"))
        {
            // We are pairing a client.
            String deviceId = parts[2];
            credentialsStore.storeClient(deviceId, key.getEncoded());
        }
        else
        {
            // We are pairing an IoT device.
            String deviceId = parts[2];

            String scopes = parts[3];
            Set<String> scopeSet = new HashSet<>();
            String[] scopeParts = scopes.split(";");
            for(String scope: scopeParts)
            {
                scopeSet.add(scope);
            }

            credentialsStore.storeRS(deviceId, key.getEncoded(), scopeSet);
        }

        udpClient.close();
    }
}
