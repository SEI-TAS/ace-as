package edu.cmu.sei.ttg.aaiot.as.pairing;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class PairingManager
{
    private static final String SCOPE_SEPARATOR = ";";

    private SecureRandom random = new SecureRandom();
    private ICredentialsStore credentialsStore;

    public PairingManager(ICredentialsStore credentialsStore)
    {
        this.credentialsStore = credentialsStore;
    }

    public boolean pair(String asID, byte[] pairingKey, String deviceIp) throws Exception
    {
        // Generate a new, random AES-128 key.
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
        String psk = Base64.getEncoder().encodeToString(newKey.getEncoded());

        // Connect to pairing device using pairing key.
        CoapsPskClient coapClient = new CoapsPskClient(deviceIp, PairingResource.PAIRING_PORT, PairingResource.PAIRING_KEY_ID, pairingKey);

        // Send our ID and the PSK to use with us.
        System.out.println("Sending pair request");
        CBORObject request = CBORObject.NewMap();
        request.Add(PairingResource.AS_ID_KEY, asID);
        request.Add(PairingResource.AS_PSK_KEY, psk);
        CBORObject reply = coapClient.sendRequest("pair", "post", request);

        System.out.println("Received reply: " + reply);
        if(reply == null)
        {
            System.out.println("Aborting pairing procedure, device did not respond.");
            coapClient.stop();
            return false;
        }

        // Get and store the device's ID, plus scopes if it was an IoT Resource Server.
        String deviceId = reply.get(PairingResource.DEVICE_ID_KEY).AsString();
        String info = reply.get(PairingResource.DEVICE_INFO_KEY).AsString();
        if(info.equals(""))
        {
            credentialsStore.storeClient(deviceId, newKey.getEncoded());
        }
        else
        {
            Set<String> scopeSet = new HashSet<>();
            String[] scopeParts = info.split(SCOPE_SEPARATOR);
            for(String scope: scopeParts)
            {
                scopeSet.add(scope);
            }

            credentialsStore.storeRS(deviceId, newKey.getEncoded(), scopeSet);
        }

        // End pairing connection.
        coapClient.stop();

        return true;
    }
}
