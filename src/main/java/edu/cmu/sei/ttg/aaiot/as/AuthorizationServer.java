package edu.cmu.sei.ttg.aaiot.as;

import COSE.*;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.as.pairing.ICredentialsStore;
import edu.cmu.sei.ttg.aaiot.config.Config;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.TimeProvider;
import se.sics.ace.as.AccessTokenFactory;
import se.sics.ace.as.DBConnector;
import se.sics.ace.coap.as.CoapDBConnector;
import se.sics.ace.coap.as.CoapsAS;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.PostgreSQLDBAdapter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sebastian on 2017-05-09.
 */
public class AuthorizationServer implements ICredentialsStore
{
    private static long HOW_LONG_TOKENS_LAST = 1000000L;

    private String asId;

    private Set<String> supportedProfiles = new HashSet<>();
    private Set<String> supportedKeyTypes = new HashSet<>();
    private Set<Integer> supportedTokenTypes = new HashSet<>();
    private Set<COSEparams> supportedCOSEParams = new HashSet<>();

    private PostgreSQLDBAdapter dbAdapter;
    private CoapsAS coapServer;
    private CoapDBConnector dbCon;
    private TimeProvider timeProvider;
    private SerializablePDP pdp;

    private String aclFilePath;

    public AuthorizationServer(String asId) throws AceException, IOException, CoseException {
        this.asId = asId;
        supportedProfiles.add("coap_dtls");
        supportedKeyTypes.add("PSK");
        supportedTokenTypes.add(AccessTokenFactory.CWT_TYPE);
        supportedCOSEParams.add(new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_64_256, AlgorithmID.Direct));

        timeProvider = new KissTime();

        dbAdapter = new PostgreSQLDBAdapter();
        dbAdapter.setParams(Config.data.get("db_user"), Config.data.get("db_pwd"), DBConnector.dbName, null);
    }

    public void createDB(String rootPwd) throws AceException
    {
        //dbAdapter.wipeDB(rootPwd);
        dbAdapter.createUser(rootPwd);
        dbAdapter.createDBAndTables(rootPwd);
    }

    public void connectToDB(String aclFilePath) throws AceException, IOException, SQLException, CoseException  {
        dbCon = new CoapDBConnector(dbAdapter, PostgreSQLDBAdapter.DEFAULT_DB_URL, Config.data.get("db_user"), Config.data.get("db_pwd"));

        this.aclFilePath = aclFilePath;
        this.pdp = new SerializablePDP(dbCon);
        this.pdp.loadFromFile(aclFilePath);
        coapServer = new CoapsAS(asId, dbCon, pdp, timeProvider, null);
    }

    public Set<String> getClients()
    {
        return pdp.getClients();
    }

    public Map<String, Set<String>> getRules(String clientId)
    {
        return pdp.getRules(clientId);
    }

    public void addRule(String clientId, String rsId, String scope) throws IOException
    {
        pdp.addRule(clientId, rsId, scope);
        pdp.saveToFile(aclFilePath);
    }

    public void removeRule(String clientId, String rsId, String scope) throws IOException
    {
        pdp.removeRule(clientId, rsId, scope);
        pdp.saveToFile(aclFilePath);
    }

    // This should be the result of the pairing procedure, adding a RS along with the shared key to use with it.
    public void addResourceServer(String rsName, OneKey PSK, Set<String> scopes) throws AceException, COSE.CoseException {
        Set<String> auds = new HashSet<>();
        auds.add(rsName);

        long resouceServerKnownExpiration = timeProvider.getCurrentTime() + HOW_LONG_TOKENS_LAST;

        dbCon.addRS(rsName, supportedProfiles, scopes, auds, supportedKeyTypes, supportedTokenTypes, supportedCOSEParams,
                resouceServerKnownExpiration, PSK, null);

        // Authorize RS to introspect.
        pdp.addRS(rsName);
        try {
            pdp.saveToFile(aclFilePath);
        }
        catch(IOException ex)
        {
            throw new AceException(ex.toString());
        }
    }

    // This should be the result of the pairing procedure, adding a client along with the shared key to use with it.
    public void addClient(String clientName, OneKey PSK) throws AceException, COSE.CoseException
    {
        dbCon.addClient(clientName, supportedProfiles, null, null, supportedKeyTypes, PSK,
                null);

        // Authorize new client to ask for tokens.
        pdp.addClient(clientName);
        try {
            pdp.saveToFile(aclFilePath);
        }
        catch(IOException ex)
        {
            throw new AceException(ex.toString());
        }
    }

    @Override
    public boolean storeClient(String id, byte[] psk)
    {
        try
        {
            System.out.println("Removing client if it was there.");
            dbCon.deleteClient(id);
            System.out.println("Adding new client " + id);
            addClient(id, createOneKeyFromBytes(psk));
            return true;
        }
        catch (Exception ex)
        {
            System.out.println("Error storing client: " + ex.toString());
            return false;
        }
    }

    @Override
    public boolean storeRS(String id, byte[] psk, Set<String> scopes)
    {
        try
        {
            System.out.println("Removing RS if it was there.");
            dbCon.deleteRS(id);
            System.out.println("Adding new RS " + id);
            addResourceServer(id, createOneKeyFromBytes(psk), scopes);
            return true;
        }
        catch (Exception ex)
        {
            System.out.println("Error storing client: " + ex.toString());
            return false;
        }
    }

    public void start()
    {
        System.out.println("Starting server");
        coapServer.start();
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
