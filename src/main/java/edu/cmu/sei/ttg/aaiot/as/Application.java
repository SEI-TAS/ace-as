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

package edu.cmu.sei.ttg.aaiot.as;

import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import edu.cmu.sei.ttg.aaiot.config.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Application
{
    public static final byte[] CLIENT_PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    // We do not need to know this, we just have it hear to simplify tests.
    public static final byte[] DEFAULT_DEVICE_PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final byte[] TEST_RS1_KEY = {(byte) 0xa1, (byte) 0xa2, (byte) 0xa3, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
    private static final byte[] TEST_RS2_KEY = {(byte) 0xb1, (byte) 0xb2, (byte) 0xb3, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
    private static final byte[] TEST_CLIENT1_KEY = {0x61, 0x62, 0x63, 0x04, 0x05, 0x06, 0x07, 0x08,
    0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
    private static final byte[] TEST_CLIENT2_KEY = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};

    private static final String CONFIG_FILE = "config.json";

    public static final String DEFAULT_CLIENT_IP = "localhost";
    public static final String DEFAULT_DEVICE_IP = "localhost";

    public static final String QR_CODE_IMAGE_FILE_PATH = "qrcode.png";
    private static final String QR_CODE_TEST_IMAGE_FILE_PATH = "qrcode_original.png";

    private static Application app = null;
    private AuthorizationServer authorizationServer;

    /**
     * Private constructor to handle the singleton pattern.
     * @throws Exception
     */
    private Application() throws Exception
    {
        try
        {
            Config.load(CONFIG_FILE);
        }
        catch(Exception ex)
        {
            System.out.println("Error loading config file: " + ex.toString());
            return;
        }

        //generateQRCode();

        // Create the server.
        String asId = Config.data.get("id");
        authorizationServer = new AuthorizationServer(asId);

        // Sets up the DB, if it has not been set up before.
        String rootPassword = Config.data.get("root_db_pwd");
        authorizationServer.createDB(rootPassword);

        // Start the server.
        authorizationServer.connectToDB();
        authorizationServer.start();

        // TODO: For testing purposes only:
        /*authorizationServer.storeClient("client1", TEST_CLIENT1_KEY);
        authorizationServer.storeClient("client2", TEST_CLIENT2_KEY);
        Set<String> scopes = new HashSet();
        scopes.add("HelloWorld");
        scopes.add("r_Lock");
        scopes.add("rw_Lock");
        authorizationServer.storeRS("RS1", TEST_RS1_KEY, scopes);
        authorizationServer.storeRS("RS2", TEST_RS2_KEY, scopes);*/
    }

    /**
     * Implements this class as a singleton.
     * @return
     * @throws Exception
     */
    public static Application getInstance() throws Exception
    {
        if(app == null)
        {
            app = new Application();
        }

        return app;
    }

    /**
     * Simple getter for the AS.
     * @return the AS instance.
     */
    public AuthorizationServer getAuthorizationServer()
    {
        return authorizationServer;
    }

    /**
     * Only used to generate the QR code image to print. Should actually be in RS...
     */
    private void generateQRCode() throws IOException
    {
        String psk = Base64.getEncoder().encodeToString(DEFAULT_DEVICE_PAIRING_KEY);
        System.out.println("Base64 encoded key: " + psk);
        QRCodeManager.createQRCodeFile(psk, QR_CODE_TEST_IMAGE_FILE_PATH);
    }
}
