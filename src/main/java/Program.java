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
            Controller controller = new Controller();
            controller.run();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}
