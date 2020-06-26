import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XsTests {
    @Test
    public void TestRandom()
    {
        XsServiceImpl server = new XsServiceImpl();
        int[] buckets = new int[10];
        for(int i=0;i<1000;i++) {
            double d = server.getRandomNumber("experiment_id - 36 . exp", UUID.randomUUID().toString());

            int bucket = (int)(d/.10);
            buckets[bucket]++;

        }
        for(int i=0;i<10;i++)
        {
            System.out.println(buckets[i]);
        }
    }

    @Test
    public void TestAssignment()
    {


        XsServiceImpl server = new XsServiceImpl();
        Map<String, ExperimentDetails> results = server.fetchQueryResults();
        HashMap<Integer,Integer> resultMap = new HashMap<>();
        for(int i=0;i<10000;i++) {
            VariationRequest vr = VariationRequest.newBuilder()
                    .setVisitId("SESSION 1")
                    .setVisitorId("VISITOR 1")
                    .setUserId(UUID.randomUUID().toString()).build();
            Variation v = server.getAssignmentForUser(vr, results, new HashMap<>(), "1");
            if(!resultMap.containsKey(v.getVariationId()))
                resultMap.put(v.getVariationId(),0);
            resultMap.put(v.getVariationId(),resultMap.get(v.getVariationId())+1);
        }

        for(int variation_id : resultMap.keySet())
        {
            System.out.println(resultMap.get(variation_id));
        }

    }
}
