import io.lettuce.core.RedisClient;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.HashMap;
import java.util.Map;

/**
 * A Singleton Class to represent a redis store
 */
public class redisTable {

    String prefix = "xs:as";
    String preformat = "{0}:{1}";
    long TTL = 7776000; // 3 months (60*60*24*30*3)
    String CONNECTION_STRING = "";
    RedisClient redisClient;
    StatefulRedisConnection<String, String> connection;

    private static redisTable _instance;

    /**
     * Fetch the instance of the redis Store
     * @return an intantiated redis instance ready to go
     */
    public static redisTable getInstance()
    {
        if(_instance==null) {
            _instance = new redisTable();
        }
        return _instance;
    }

    /**
     * Private contructor for singleton
     */
    private redisTable()
    {
        RedisClient.create(CONNECTION_STRING);
        connection = redisClient.connect();
    }

    /**
     * Synchronous.  Fetch the Persisted Map of Experiment_id,Variation_id in a .
     * If either the user_id or visitor_id matches, it will pull that back
     * @param user_id The user_id of the user (if it exists, null or empty otherwise)
     * @param visitor_id The visitor_id of the user (if it exists, null or empty otherwise)
     * @return a Map with Key = Experiment_id Value = Variation_id for the user
     */
    public Map<String,String> get(String user_id,String visitor_id)
    {
        try {
            RedisCommands<String, String> syncCommands = connection.sync();
            syncCommands.multi();
            syncCommands.hgetall(String.format(preformat, prefix, user_id));
            syncCommands.hgetall(String.format(preformat, prefix, visitor_id));
            TransactionResult tr = syncCommands.exec();
            Map<String, String> user_id_fetch = tr.get(0);
            Map<String, String> visitor_id_fetch = tr.get(1);
            Map<String, String> existing_map;
            if(user_id_fetch != null)
                return user_id_fetch;
            if(visitor_id_fetch != null)
            {
                return visitor_id_fetch;
            }
            return   new HashMap<String,String>();

        }
        catch (Exception E)
        {

        }
        finally {
            return  new HashMap<String,String>();
        }


    }

    /**
     * A Synchronous request that Saves the variation to redis
     * @param user_id the user_id we are saving for
     * @param visitor_id the visitor_id we are saving for
     * @param experiment_id the experiment_id that was exposed
     * @param variation_id the variation_id that was exposed
     */
    public void saveSync(String  user_id,String visitor_id,int experiment_id, int variation_id)
    {

        RedisCommands<String, String> syncCommands = connection.sync();
        syncCommands.multi();
        syncCommands.hgetall(String.format(preformat, prefix, user_id));
        syncCommands.hgetall(String.format(preformat, prefix, visitor_id));
        TransactionResult tr = syncCommands.exec();
        Map<String, String> user_id_fetch = tr.get(0);
        Map<String, String> visitor_id_fetch = tr.get(1);
        Map<String, String> existing_map;

        if (user_id != null && !user_id.isEmpty() && user_id_fetch !=null  && user_id_fetch.size() > 0 )
        {
            existing_map=user_id_fetch;
        } else if(visitor_id_fetch !=null  && visitor_id_fetch.size() > 0)
        {
            existing_map=visitor_id_fetch;
        } else
        {
            existing_map = new HashMap<>();
        }

        String unit_id;
        if(user_id!=null && !user_id.isEmpty()){
            unit_id=user_id;
        }else{
            unit_id=visitor_id;
        }

        existing_map.put(String.valueOf(experiment_id),String.valueOf(variation_id));
        String key = String.format(preformat, prefix, unit_id);

        syncCommands.multi();
        syncCommands.hset(key,existing_map);
        syncCommands.expire(key,TTL);
        syncCommands.exec();

    }
}
