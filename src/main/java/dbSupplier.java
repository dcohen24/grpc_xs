import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class dbSupplier {

    private  static Connection conn;
    private  static Object lock = new Object();

    /**
     * Fetch All data from the experiment service database.
     * This is setup as static so that it can be easily cached
     * @return a Map that holds <Experiment_id,Details of Experiment>
     */
    public static Map<String,ExperimentDetails> fetchQueryResults() {
        try {
            if(conn==null || conn.isClosed()) {
                synchronized (lock) {
                    if(conn==null || conn.isClosed()) {
                        String url = "jdbc:postgresql://localhost/xs";
                        Properties props = new Properties();
                        props.setProperty("user", "postgres");
                        props.setProperty("password", "admin");
                        conn = DriverManager.getConnection(url, props);
                    }
                }
            }

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(getQueryText());
            int lastExperimentId=-999;
            int lastVariationId = -999;
            boolean isFirstExperiment=true;
            boolean isFirstVariation=true;

            HashMap<String,ExperimentDetails> fullSet = new HashMap<>();
            ExperimentDetails buildup = null;
            Variation.Builder variationBuilder = Variation.newBuilder();

            while( rs.next())
            {
                int variationId=rs.getInt("variation_id");
                int experimentId=rs.getInt("experiment_id");
                String variationName =rs.getString("variation_name");
                double weight =rs.getDouble("weight");
                boolean isControl = rs.getBoolean("is_control");
                boolean isActive = rs.getBoolean("is_active");
                boolean isHoldout = rs.getBoolean("is_holdout");
                boolean isWinning = rs.getBoolean("is_winning");
                int appId=rs.getInt("app_id");
                int ownerId=rs.getInt("owner_id");
                String attributeName =rs.getString("attribute_name");
                String attributeValue =rs.getString("attribute_value");


                if(experimentId!=lastExperimentId) { //Handle New Experiment
                    if (!isFirstExperiment) {
                        buildup.addVariation(variationBuilder.build());
                        fullSet.put(String.valueOf(lastExperimentId), buildup);
                        isFirstVariation=true;
                    } else {
                        isFirstExperiment = false;
                    }

                    buildup = new ExperimentDetails(String.valueOf(experimentId));

                    lastExperimentId = experimentId;
                }
                if(variationId!=lastVariationId) { //Handle New Variation
                    if(!isFirstVariation)
                    {
                        buildup.addVariation(variationBuilder.build());
                    }
                    else
                        isFirstVariation=false;
                    lastVariationId = variationId;

                    variationBuilder =  Variation.newBuilder();
                    variationBuilder.setExperimentId(experimentId);
                    variationBuilder.setIsActive(isActive);
                    variationBuilder.setIsControl(isControl);
                    variationBuilder.setIsWinning(isWinning);
                    variationBuilder.setWeight(weight);
                    variationBuilder.setVariationId(variationId);
                    variationBuilder.setIsHoldout(isHoldout);

                }
                //Now we just need to add new attributes to the builder
                variationBuilder.addAttribute(Variation.VariationAttributes.newBuilder()
                        .setAttributeName(attributeName)
                        .setAttributeValue(attributeValue)
                        .build()
                );
            }
            rs.close();
            //Add the last experiment and the Last variation
            buildup.addVariation(variationBuilder.build());
            fullSet.put(String.valueOf(lastExperimentId), buildup);
            return fullSet;


        }
        catch (Exception E)
        {

        }


        return  new HashMap<String, ExperimentDetails>();

    }

    private static String  getQueryText()
    {
        String S = "\n" +
                " select \n" +
                "\tV.variation_id\n" +
                "\t,V.experiment_id\n" +
                "\t,V.variation_name\n" +
                "\t,V.weight\n" +
                "\t,V.is_control\n" +
                "\t,V.is_holdout\n" +
                "\t,V.is_winning\n" +
                "\t,V.is_active\n" +
                "\t,E.app_id\n" +
                "\t,E.owner_id\n" +
                "\t,VA.attribute_name\n" +
                "\t,VA.attribute_value\n" +
                "from variation V\n" +
                ",\texperiment E\n" +
                ",variation_attribute VA\n" +
                "where V.experiment_id=E.experiment_id\n" +
                "and VA.variation_id=V.variation_id\n" +
                "order by E.experiment_id,V.variation_id,variation_attribute_id";
        return  S;
    }
}
