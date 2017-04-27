/**
 * Created by Sebastian on 2017-03-10.
 */

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.*;
import se.sics.ace.as.DBConnector;
import se.sics.ace.coap.as.CoapDBConnector;
import se.sics.ace.coap.as.CoapsAS;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.PostgreSQLDBCreator;
import se.sics.ace.examples.KissTime;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Base64;


public class Test {
    static byte[] key256 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};

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

        OneKey asKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        CoapDBConnector dbCon = new CoapDBConnector(PostgreSQLDBCreator.DEFAULT_DB_URL, userName, userPwd);
        CoapsAS authorizationServer = new CoapsAS("myid", dbCon,
                KissPDP.getInstance("src/main/resources/acl.json", dbCon), new KissTime(), null);

        Set<String> profiles = new HashSet<>();
        profiles.add("coap_dtls");
        Set<String> keyTypes = new HashSet<>();
        keyTypes.add("PSK");
        //keyTypes.add("RPK");

        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(key256));
        OneKey sharedKey = new OneKey(keyData);

        //String publicKeyStr = "piJYICg7PY0o/6Wf5ctUBBKnUPqN+jT22mm82mhADWecE0foI1ghAKQ7qn7SL/Jpm6YspJmTWbFG8GWpXE5GAXzSXrialK0pAyYBAiFYIBLW6MTSj4MRClfSUzc8rVLwG8RH5Ak1QfZDs4XhecEQIAE=";
        //OneKey publickey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(publicKeyStr)));
        dbCon.addClient("clientA", profiles, null, null, keyTypes, sharedKey,
                null);

        System.out.println("Starting server");
        authorizationServer.start();
    }
}
