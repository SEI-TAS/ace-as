import COSE.*;
import com.upokecenter.cbor.CBORObject;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.TimeProvider;
import se.sics.ace.as.AccessTokenFactory;
import se.sics.ace.as.DBConnector;
import se.sics.ace.coap.as.CoapDBConnector;
import se.sics.ace.coap.as.CoapsAS;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.PostgreSQLDBAdapter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sebastian on 2017-05-09.
 */
public class AuthorizationServer {
    private static byte[] sharedKey256Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
    private OneKey sharedKey;

    private static long howLongTokensLast = 1000000L;

    private final String userName = "aceuser";
    private final String userPwd = "pwd";
    private final String rootPwd = "z5imVxzKw";

    private Set<String> supportedProfiles = new HashSet<>();
    private Set<String> supportedKeyTypes = new HashSet<>();
    private Set<Integer> supportedTokenTypes = new HashSet<>();
    private Set<COSEparams> supportedCOSEParams = new HashSet<>();

    private PostgreSQLDBAdapter dbAdapter;
    private CoapsAS coapServer;
    private CoapDBConnector dbCon;
    private TimeProvider timeProvider;

    public AuthorizationServer() throws AceException, IOException, CoseException {
        supportedProfiles.add("coap_dtls");
        supportedKeyTypes.add("PSK");
        supportedTokenTypes.add(AccessTokenFactory.CWT_TYPE);
        supportedCOSEParams.add(new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_64_256, AlgorithmID.Direct));

        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(sharedKey256Bytes));
        sharedKey = new OneKey(keyData);

        timeProvider = new KissTime();

        dbAdapter = new PostgreSQLDBAdapter();
        dbAdapter.setParams(userName, userPwd, DBConnector.dbName, null);
    }

    public void createDB() throws AceException
    {
        dbAdapter.createUser(rootPwd);
        dbAdapter.createDBAndTables(rootPwd);
    }

    public void connectToDB() throws AceException, IOException, SQLException, CoseException  {
        dbCon = new CoapDBConnector(dbAdapter, PostgreSQLDBAdapter.DEFAULT_DB_URL, userName, userPwd);

        // TODO: check if this asymetric key is actually needed if RPK is not used....
        //OneKey asKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        coapServer = new CoapsAS("TestAS", dbCon,
                KissPDP.getInstance("src/main/resources/acl.json", dbCon), timeProvider, null);

    }

    public void storeClientsAndResourceServers() throws AceException
    {
        //OneKey key = OneKey.generateKey(AlgorithmID.ECDSA_256);
        //OneKey rsPublicKey = key.PublicKey();
        Set<String> auds = new HashSet<>();
        auds.add("rs1");
        Set<String> scopes = new HashSet<>();
        scopes.add("r_temp");
        scopes.add("co2");
        long resouceServerKnownExpiration = timeProvider.getCurrentTime() + howLongTokensLast;
        dbCon.addRS("rs1", supportedProfiles, scopes, auds, supportedKeyTypes, supportedTokenTypes, supportedCOSEParams,
                resouceServerKnownExpiration, sharedKey, null);

        //String publicKeyStr = "piJYICg7PY0o/6Wf5ctUBBKnUPqN+jT22mm82mhADWecE0foI1ghAKQ7qn7SL/Jpm6YspJmTWbFG8GWpXE5GAXzSXrialK0pAyYBAiFYIBLW6MTSj4MRClfSUzc8rVLwG8RH5Ak1QfZDs4XhecEQIAE=";
        //OneKey acPublickey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(publicKeyStr)));
        dbCon.addClient("clientA", supportedProfiles, null, null, supportedKeyTypes, sharedKey,
                null);
    }

    public void start()
    {
        System.out.println("Starting server");
        coapServer.start();
    }
}
