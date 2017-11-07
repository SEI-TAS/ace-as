package edu.cmu.sei.ttg.aaiot.as;

import edu.cmu.sei.ttg.aaiot.as.commandline.CommandLineUI;
import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import edu.cmu.sei.ttg.aaiot.config.Config;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Starter
{
    public static final byte[] CLIENT_PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    // We do not need to know this, we just have it hear to simplify tests.
    public static final byte[] PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final String CONFIG_FILE = "config.json";

    public static final String DEFAULT_CLIENT_IP = "localhost";
    public static final String DEFAULT_DEVICE_IP = "localhost";

    public static final String QR_CODE_IMAGE_FILE_PATH = "qrcode.png";
    private static final String QR_CODE_TEST_IMAGE_FILE_PATH = "qrcode_original.png";

    private AuthorizationServer authorizationServer;

    public Starter() throws Exception
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

        String rootPassword = Config.data.get("root_db_pwd");

        //generateQRCode();

        // Create the server and its DB.
        String asId = Config.data.get("id");
        authorizationServer = new AuthorizationServer(asId);
        authorizationServer.createDB(rootPassword);
        authorizationServer.connectToDB();

        // Start the server.
        authorizationServer.start();
    }

    public AuthorizationServer getAuthorizationServer()
    {
        return authorizationServer;
    }

    // Only used to generate the QR code image to print. Should actually be in RS...
    private void generateQRCode() throws IOException
    {
        String psk = Base64.getEncoder().encodeToString(PAIRING_KEY);
        System.out.println("Base64 encoded key: " + psk);
        QRCodeManager.createQRCodeFile(psk, QR_CODE_TEST_IMAGE_FILE_PATH);
    }
}
