package edu.cmu.sei.ttg.aaiot.as;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import se.sics.ace.AceException;
import se.sics.ace.as.DBConnector;
import se.sics.ace.as.PDP;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-08-04.
 */
public class SerializablePDP implements PDP, AutoCloseable {

    private DBConnector db = null;

    // The identifiers of the devices allowed to submit requests to /token.
    private Set<String> tokenAllowedDevices = new HashSet<>();

    // The identifiers of the devices allowed to submit requests to /introspect.
    private Set<String> introspectAllowedDevices = new HashSet<>();

    /**
     * Maps identifiers of client to a map that maps the audiences they may
     * access to the scopes they may access for these audiences.
     */
    private Map<String, Map<String, Set<String>>> acl = new HashMap<>();

    // File where all this info is permanently stored.
    private String aclFile;

    /**
     * @param db  the database connector
     * @param aclFile the path and filename to the file where this will be stored.
     * @return  the PDP
     */
    public SerializablePDP(DBConnector db, String aclFile)
    {
        this.db = db;
        this.aclFile = aclFile;
    }

    @Override
    public boolean canAccessToken(String deviceId)
    {
        return this.tokenAllowedDevices.contains(deviceId);
    }

    @Override
    public boolean canAccessIntrospect(String deviceId)
    {
        return this.introspectAllowedDevices.contains(deviceId);
    }

    @Override
    public String canAccess(String clientId, String aud, String scope)
            throws AceException
    {
        Map<String,Set<String>> clientACL = this.acl.get(clientId);
        if (clientACL == null || clientACL.isEmpty()) {
            return null;
        }

        Set<String> scopes = null;
        Set<String> rss = this.db.getRSS(aud);
        if (rss == null) {
            return null;
        }
        for (String rs : rss) {
            if (scopes == null) {
                scopes = new HashSet<>();
                Set<String> bar = clientACL.get(rs);
                if (bar != null) {
                    scopes.addAll(bar);
                }
            } else {
                Set<String> remains = new HashSet<>(scopes);
                for (String foo : scopes) {
                    if (clientACL.get(rs) == null ) {
                        //The client can access nothing on this RS
                        return null;
                    }
                    if (!clientACL.get(rs).contains(foo)) {
                        remains.remove(foo);
                    }
                }
                scopes = remains;
            }
        }

        if (scopes == null || scopes.isEmpty()) {
            return null;
        }

        String scopeStr = scope;
        String[] requestedScopes = scopeStr.split(" ");
        String grantedScopes = "";
        for (int i=0; i<requestedScopes.length; i++) {
            if (scopes.contains(requestedScopes[i])) {
                if (!grantedScopes.isEmpty()) {
                    grantedScopes += " ";
                }
                grantedScopes += requestedScopes[i];
            }
        }

        // All scopes found
        if (grantedScopes.isEmpty()) {
            return null;
        }

        return grantedScopes;
    }

    @Override
    public void close() throws Exception
    {
        this.db.close();
    }

    public void addTokenDevice(String deviceId)
    {
        if(!tokenAllowedDevices.contains(deviceId))
        {
            tokenAllowedDevices.add(deviceId);
        }
    }

    public void removeTokenDevice(String deviceId)
    {
        if(tokenAllowedDevices.contains(deviceId))
        {
            tokenAllowedDevices.remove(deviceId);
        }
    }

    public void addIntrospectDevice(String deviceId)
    {
        if(!introspectAllowedDevices.contains(deviceId))
        {
            introspectAllowedDevices.add(deviceId);
        }
    }

    public void removeIntrospectDevice(String deviceId)
    {
        if(introspectAllowedDevices.contains(deviceId))
        {
            introspectAllowedDevices.remove(deviceId);
        }
    }

    public void addRule(String clientId, String rsId, String scope)
    {
        Map<String, Set<String>> permissions;
        if(acl.containsKey(clientId))
        {
            permissions = acl.get(clientId);
        }
        else
        {
            permissions = new HashMap<>();
            acl.put(clientId, permissions);
        }

        Set<String> scopes;
        if(permissions.containsKey(rsId))
        {
            scopes = permissions.get(rsId);
        }
        else
        {
            scopes = new HashSet<>();
            permissions.put(rsId, scopes);
        }

        if(!scopes.contains(scope))
        {
            scopes.add(scope);
        }
    }

    public void removeRule(String clientId, String rsId, String scope)
    {
        acl.get(clientId).get(rsId).remove(scope);
    }

    public void clearRSRules(String rsId)
    {
        for(String clientId : acl.keySet())
        {
            Map<String, Set<String>> rsRules = acl.get(clientId);
            if(rsRules.containsKey(rsId))
            {
                rsRules.remove(rsId);
            }
        }
    }

    public void clearClientRules(String clientId)
    {
        if(acl.containsKey(clientId))
        {
            acl.remove(clientId);
        }
    }

    public Set<String> getTokenAllowedDevices()
    {
        return tokenAllowedDevices;
    }

    public Set<String> getIntrospectAllowedDevices()
    {
        return introspectAllowedDevices;
    }

    public Map<String, Set<String>> getRules(String clientId)
    {
        return acl.get(clientId);
    }

    public void loadFromFile() throws AceException, IOException
    {
        FileInputStream fs;
        try
        {
            fs = new FileInputStream(aclFile);
        }
        catch(IOException ex)
        {
            System.out.println("ACL file " + aclFile + " not found, will be created.");
            return;
        }

        JSONTokener parser = new JSONTokener(fs);
        JSONArray fileData = new JSONArray(parser);

        // Parse the clients allowed to access this AS
        if (!(fileData.get(0) instanceof JSONArray))
        {
            fs.close();
            throw new AceException("Invalid PDP configuration");
        }
        JSONArray clientsJ = (JSONArray)fileData.get(0);
        tokenAllowedDevices = new HashSet<>();
        Iterator<Object> it = clientsJ.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (next instanceof String) {
                tokenAllowedDevices.add((String)next);
            } else {
                fs.close();
                throw new AceException("Invalid PDP configuration");
            }
        }

        //Parse the RS allowed to access this AS
        if (!(fileData.get(1) instanceof JSONArray)) {
            fs.close();
            throw new AceException("Invalid PDP configuration");
        }
        JSONArray rsJ = (JSONArray)fileData.get(1);
        introspectAllowedDevices = new HashSet<>();
        it = rsJ.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (next instanceof String) {
                introspectAllowedDevices.add((String)next);
            } else {
                fs.close();
                throw new AceException("Invalid PDP configuration");
            }
        }

        //Read the acl
        if (!(fileData.get(2) instanceof JSONObject)) {
            fs.close();
            throw new AceException("Invalid PDP configuration");
        }
        JSONObject aclJ = (JSONObject)fileData.get(2);
        acl = new HashMap<>();
        Iterator<String> clientACL = aclJ.keys();
        //Iterate through the client_ids
        while(clientACL.hasNext()) {
            String client = clientACL.next();
            if (!(aclJ.get(client) instanceof JSONObject)) {
                fs.close();
                throw new AceException("Invalid PDP configuration");
            }
            Map<String, Set<String>> audM = new HashMap<>();
            JSONObject audJ = (JSONObject) aclJ.get(client);
            Iterator<String> audACL = audJ.keys();
            //Iterate through the audiences
            while(audACL.hasNext()) {
                String aud = audACL.next();
                if (!(audJ.get(aud) instanceof JSONArray)) {
                    fs.close();
                    throw new AceException("Invalid PDP configuration");
                }
                Set<String> scopeS = new HashSet<>();
                JSONArray scopes = (JSONArray)audJ.get(aud);
                Iterator<Object> scopeI = scopes.iterator();
                //Iterate through the scopes
                while (scopeI.hasNext()) {
                    Object scope = scopeI.next();
                    if (!(scope instanceof String)) {
                        fs.close();
                        throw new AceException("Invalid PDP configuration");
                    }
                    scopeS.add((String)scope);
                }
                audM.put(aud, scopeS);
            }
            acl.put(client, audM);
        }
        fs.close();
    }

    public void saveToFile() throws IOException
    {
        JSONArray clientsArray = new JSONArray(tokenAllowedDevices);
        JSONArray rsArray = new JSONArray(introspectAllowedDevices);
        JSONObject aclObject = new JSONObject(acl);

        JSONArray mainArray = new JSONArray();
        mainArray.put(clientsArray);
        mainArray.put(rsArray);
        mainArray.put(aclObject);

        FileWriter file = new FileWriter(aclFile, false);
        file.write(mainArray.toString());
        file.flush();
        file.close();
    }

    public void wipe() throws IOException
    {
        tokenAllowedDevices.clear();
        introspectAllowedDevices.clear();
        acl.clear();

        // Save to file to save an empty file.
        saveToFile();
    }
}