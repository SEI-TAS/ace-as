/**
 * Created by Sebastian on 2017-03-10.
 */

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.OneKey;
import se.sics.ace.*;
import se.sics.ace.as.DBConnector;
import se.sics.ace.coap.as.CoapDBConnector;
import se.sics.ace.coap.as.CoapsAS;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.PostgreSQLDBCreator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;


public class Test {
    public static void main(String[] args)
    {
        try {
            Test.t1();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    public static void t1() throws SQLException, AceException, IOException, CoseException {
        String userName = "aceuser";
        String userPwd = "pwd";
        String rootPwd = "z5imVxzKw";
        PostgreSQLDBCreator creator = new PostgreSQLDBCreator();
        creator.setParams(userName, userPwd, DBConnector.dbName, null);
        creator.createUser(rootPwd);
        creator.createDBAndTables(rootPwd);

        Set<String> keyTypes = new HashSet<>();
        keyTypes.add("PSK");
        keyTypes.add("RPK");

        OneKey publicKey;
        OneKey key = OneKey.generateKey(AlgorithmID.ECDSA_256);
        publicKey = key.PublicKey();

        CoapDBConnector dbCon = new CoapDBConnector(PostgreSQLDBCreator.DEFAULT_DB_URL, userName, userPwd);
        CoapsAS authorizationServer = new CoapsAS("myid", dbCon,
                KissPDP.getInstance("src/main/resources/acl.json", dbCon), null, null);

        Set<String> profiles = new HashSet<>();
        profiles.add("coap_dtls");
        keyTypes.clear();
        keyTypes.add("RPK");
        dbCon.addClient("clientA", profiles, null, null, keyTypes, null,
                publicKey);

        System.out.println("Starting server");
        authorizationServer.start();
    }
}
