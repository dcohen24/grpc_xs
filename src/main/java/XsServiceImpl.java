import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.errorprone.annotations.Var;
import io.grpc.stub.StreamObserver;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/*https://grpc.io/docs/languages/java/basics/ */
public class XsServiceImpl extends XsServiceGrpc.XsServiceImplBase {


    double  maxByteValue =0;
    Connection conn;
    Supplier<Map<String,ExperimentDetails>> memoizedDBQuery;
    redisTable redis;
    public XsServiceImpl()
    {
        super();
        byte[] maxValue= new byte[20];
        Arrays.fill(maxValue, (byte) 0xff);
        maxByteValue = (new BigInteger(1,maxValue)).doubleValue();
        memoizedDBQuery = Suppliers.memoizeWithExpiration(dbSupplier::fetchQueryResults,5, TimeUnit.MINUTES);
        redis= redisTable.getInstance();

    }
    @Override
    public void getVariations(VariationRequest request, StreamObserver<Variation> responseObserver) {
        Map<String, ExperimentDetails> queryResults = memoizedDBQuery.get();
        Map<String,String> stickyAssignments = getStickyAssignmentsFromRedis(request.getUserId());

        for (String experiment_id : queryResults.keySet()) {
            Variation returner =  getAssignmentForUser(request, queryResults, stickyAssignments, experiment_id);
            responseObserver.onNext(returner);

        }
        responseObserver.onCompleted();
    }

    @Override
    public void logExposure(ExposureRequest request, StreamObserver<ExposureReply> responseObserver) {
        Map<String, ExperimentDetails> queryResults = memoizedDBQuery.get();
        String ExpIdStringValue = String.valueOf(request.getExperimentId());
        if(!queryResults.containsKey(ExpIdStringValue))
        {
            ExposureReply reply = ExposureReply.newBuilder().setIsOk(false).build();
        }
        ExperimentDetails details=  queryResults.get(ExpIdStringValue);
        ExposureReply reply = ExposureReply.newBuilder().setIsOk(true).build();
        if(details.isWinnerSelected()) {
            ;
        }
        Variation variationToLog = details.getVariationsByVariationId().get(request.getVariationId());

        if(variationToLog.getIsActive()&& !variationToLog.getIsHoldout())
        {
            //Write to Persisence (Redis!) --TODO: Could flip to Async
            redis.saveSync(request.getUserId(),request.getVisitorId(),request.getExperimentId(),request.getVariationId());
        }

        if(variationToLog.getIsActive()) //LogMeToKafka
        {
            //TODO: Write Kafka output for logging variations
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    /**
     * a Cached (Memoized) Query against the database
     * Shouldn't have much impact in anything but the 1 query every 5 minutes
     *
     * @return Map from Experiment_id -> ExperimentDetails
     */
    protected Map<String, ExperimentDetails> fetchQueryResults()
    {
        return memoizedDBQuery.get();
    }

    protected Variation getAssignmentForUser(VariationRequest request
            , Map<String, ExperimentDetails> queryResults
            , Map<String, String> stickyAssignments
            , String experiment_id) {
        Variation returner;
        ExperimentDetails experimentDetails = queryResults.get(experiment_id);

        //If there is a winner, take that
        if(experimentDetails.isWinnerSelected())
            returner = experimentDetails.getWinningVariation();
        //Otherwise CHeck sticky

        else if (stickyAssignments.containsKey(experiment_id)) {
            String assignedVariationIdAsString= stickyAssignments.get(experiment_id);
            returner = experimentDetails.getVariationsByStringVariationId().get(assignedVariationIdAsString);
        }
        //Random Coin Flip
        else {
            returner = getVariationAssignment(experiment_id, queryResults.get(experiment_id), request);
        }
        //IF the cell has been deactivated serve the control content, but keep the user in the
        //original cell to preserve the control data
        if(!returner.getIsActive())
        {
            Variation control = experimentDetails.getControlVariation();
            returner = replaceAttributeList(returner, control);
        }
        return returner;
    }

    protected Map<String,String>getStickyAssignmentsFromRedis(String userId)
    {
        Map m = new HashMap<String,String>();
        return m;
    }

    protected Variation replaceAttributeList(Variation input, Variation control) {
        Variation replacement  = Variation.newBuilder()
                .setExperimentId(input.getExperimentId())
                .setIsActive(input.getIsActive())
                .setIsWinning(input.getIsWinning())
                .setVariationId(input.getVariationId())
                .setWeight(input.getWeight())
                .setIsControl(input.getIsControl())
                .addAllAttribute(control.getAttributeList())
                .build();
        return  replacement;

    }

    protected Variation getVariationAssignment(String experiment_id
            ,  ExperimentDetails details
            , VariationRequest request) {

        String salt = String.format("experiment_id - %s . exp", experiment_id);

        String id_to_use = getUnitId(request);

        Variation assignment = null;
        double zero_to_one= getRandomNumber(salt, id_to_use);
        double stop_value = details.getActiveWeight()*zero_to_one;
        List<Double> cumWeights = details.getCumulativeWeights();
        for(int i=0;i<cumWeights.size();i++)
        {
            if(stop_value <= cumWeights.get(i))
            {
                assignment=details.getActiveVariationsAsList().get(i);
                break;
            }

        }
        return assignment;
    }




    protected double getRandomNumber(String salt, String id_to_use) {
        double value;
        String hashString = String.format("%s.%s",salt,id_to_use);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(hashString.getBytes("ascii"));
            byte[] shaArray = digest.digest();

            double d = new BigInteger(1,shaArray).doubleValue();
            value = d/maxByteValue;
            return  value;


        }
        catch (Exception E)
        {

        }
        finally {
            //Should never happen!

        }
        return 0;
    }

    protected String getUnitId(VariationRequest request) {
        String id_to_use= request.getVisitorId();
        String user_id_value = request.getUserId();
            if(user_id_value!=null && !user_id_value.isEmpty() )
            {
                id_to_use=user_id_value;
            }
        return id_to_use;
    }





}


