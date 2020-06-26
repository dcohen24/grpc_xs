import com.google.errorprone.annotations.Var;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentDetails {
    private String experimentId;
    private Variation controlVariation;
    private Variation winningVariation;
    private boolean isWinnerSelected;
    private Map<Integer,Variation> variationMap;
    private List<Variation> activeVariations;
    private List<Double> cumulativeWeights;
    private double activeWeight;
    private Map<String,Variation>  variationStringMap;
    public ExperimentDetails(String experimentId)
    {

        variationMap=new HashMap<Integer, Variation>();
        activeVariations = new ArrayList<Variation>();
        this.experimentId=experimentId;
        activeWeight=0;
        this.isWinnerSelected=false;
        this.cumulativeWeights = new ArrayList<>();
        variationStringMap =new HashMap<String, Variation>();



    }

    /**
     * Getter: gets Experiment_id (a number) as a string
     * @return The Experiment_id
     */
    public String getExperimentId() {
        return experimentId;
    }

    /**
     * Gets the Variation protobuf that is a control
     * @return A Variation
     */
    public Variation getControlVariation() {
        return controlVariation;
    }

    /**
     * Gets the Variations, with experiment_id represented as a String
     * @return a map with Experiment_id(as a string) and variation_id as the value
     */
    public Map<String,Variation> getVariationsByStringVariationId() {
        return variationStringMap;
    }

    /**
     * Gets the Variations, with experiment_id represented as a String
     * @return a map with Experiment_id(as an int) and variation_id as the value
     * */
    public Map<Integer,Variation> getVariationsByVariationId() {
        return variationMap;
    }

    /**
     * Gets a List of the cumulative weights up to an index.
     * Used for the random coinflip
     * @return a List<Double> with weights
     */
    public List<Double> getCumulativeWeights() {
        return cumulativeWeights;
    }

    /**
     * Returns the total weight for all active variations
     * @return a double
     */
    public double getActiveWeight() {
        return activeWeight;
    }


    public List<Variation> getActiveVariationsAsList() {
        return activeVariations;
    }


    public Variation getWinningVariation() {
        return winningVariation;
    }


    public boolean isWinnerSelected() {
        return isWinnerSelected;
    }

    /**
     * Adds a variation, and keeps all the internal variations up to date
     * @param V A variation to be added
     */
    public void addVariation(Variation V)
    {
        if(V.getIsControl() && this.controlVariation==null)
            this.controlVariation=V;
        if(V.getIsWinning() && this.winningVariation==null) {
            this.winningVariation = V;
            this.isWinnerSelected = true;
        }
        if(V.getIsActive()) {
            this.activeWeight += V.getWeight();
            this.cumulativeWeights.add(this.activeWeight);
            this.activeVariations.add(V);
        }

        this.variationMap.put(V.getVariationId(),V);




    }
}
