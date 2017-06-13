import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sebastian on 2017-03-10.
 */

public class Program {
    public static void main(String[] args)
    {
        try {
            AuthorizationServer as = new AuthorizationServer();
            as.createDB();
            as.connectToDB();

            Set<String> scopes = new HashSet<>();
            scopes.add("r_temp");
            scopes.add("co2");

            as.addResourceServer("rs1", scopes);
            as.addClient("clientA");

            as.start();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }
}
