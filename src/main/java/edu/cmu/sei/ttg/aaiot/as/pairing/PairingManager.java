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
        byte[] psk = newKey.getEncoded();

        // Connect to pairing device using pairing key.
        CoapsPskClient coapClient = new CoapsPskClient(deviceIp, PairingResource.PAIRING_PORT, PairingResource.PAIRING_KEY_ID, pairingKey);

        // Send our ID and the PSK to use with us.
        System.out.println("Sending pair request");
        CBORObject request = CBORObject.NewMap();
        request.Add(CBORObject.FromObject(PairingResource.AS_ID_KEY), asID);
        request.Add(CBORObject.FromObject(PairingResource.AS_PSK_KEY), psk);
        System.out.println("Request being sent as CBOR: " + request.toString());
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
