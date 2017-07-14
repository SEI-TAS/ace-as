import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sebastian on 2017-03-10.
 */

public class Program {
    public static void main(String[] args)
    {
        try {
            // TODO: load from somewhere.
            String rootPassword = "";

            // Create the server and its DB.
            AuthorizationServer as = new AuthorizationServer();
            as.createDB(rootPassword);
            as.connectToDB();

            // TODO: This should be called as the result of pairing.
            byte[] rsSharedKey256Bytes = {'b', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
            OneKey rsPSK = createOneKeyFromBytes(rsSharedKey256Bytes);
            Set<String> scopes = new HashSet<>();
            scopes.add("r_temp");
            scopes.add("co2");
            as.addResourceServer("rs1", rsPSK, scopes);

            // TODO: This should be called as the result of pairing.
            byte[] clientAsharedKey256Bytes = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};
            OneKey clientAPSK = createOneKeyFromBytes(clientAsharedKey256Bytes);
            as.addClient("clientA", clientAPSK);

            // Start the server.
            as.start();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    private static OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        OneKey key = new OneKey(keyData);
        return key;
    }

}
