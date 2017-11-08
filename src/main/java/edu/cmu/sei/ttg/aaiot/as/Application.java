package edu.cmu.sei.ttg.aaiot.as;

import edu.cmu.sei.ttg.aaiot.as.pairing.QRCodeManager;
import edu.cmu.sei.ttg.aaiot.config.Config;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 7/18/17.
 */
public class Application
{
    public static final byte[] CLIENT_PAIRING_KEY = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    // We do not need to know this, we just have it hear to simplify tests.
    public static final byte[] DEFAULT_DEVICE_PAIRING_KEY = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

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
