package org.dice_group.lpbenchgen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dice_group.lpbenchgen.dl.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The overall Configuration.
 *
 * @author Lixi Alié Conrads
 */
public class Configuration {

    /**
     * Loads the configuration from a yaml file.
     *
     * @param file the file
     * @return the configuration
     * @throws IOException the io exception
     */
    public static Configuration loadFromFile(String file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(file), Configuration.class);
    }

    // GENERAL PARAMETERS
    @JsonProperty
    private List<String> types = new ArrayList<>();
    @JsonProperty(defaultValue = "0.5")
    private Double percentageOfPositiveExamples=0.5;
    @JsonProperty(defaultValue = "0.5")
    private Double percentageOfNegativeExamples=0.5;
    @JsonProperty(defaultValue = "1")
    private Integer seed=1;
    @JsonProperty(defaultValue = "30")
    private Integer maxNoOfExamples=30;
    @JsonProperty(defaultValue = "5")
    private Integer minNoOfExamples=5;
    @JsonProperty(defaultValue = "100")
    private Integer aboxResultRetrievalLimit=100;
    @JsonProperty(defaultValue = "0.5")
    private Double splitContainment=0.5;
    @JsonProperty(required = true)
    private String endpoint;
    @JsonProperty(required = true)
    private String owlFile;
    @JsonProperty
    private boolean openWorldAssumption = false;
    @JsonProperty
    private boolean removeLiterals=false;

    @JsonProperty
    private List<PosNegExample> concepts;

    // GENERATION PARAMETERS
    @JsonProperty(defaultValue = "20")
    private Integer maxGenerateConcepts=20;
    @JsonProperty(defaultValue = "2")
    private Integer maxDepth=2;
    @JsonProperty(defaultValue = "10")
    private Integer maxConceptLength=10;
    @JsonProperty(defaultValue = "4")
    private Integer minConceptLength=4;
    @JsonProperty(defaultValue = "0")
    private Integer positiveLimit=0;
    @JsonProperty(defaultValue = "100")
    private Integer negativeLimit=100;
    @JsonProperty(defaultValue = "true")
    private Boolean inferDirectSuperClasses=true;
    @JsonProperty
    private String namespace;
    @JsonProperty
    private boolean strict=false;
    @JsonProperty
    private Double negationMutationRatio=0.0;

    /**
     * An internal limit for retrieving only this amount of query solutions.
     * Will determine mostly how big the ABox will be.
     *
     * @return aboxResultRetrievalLimit
     */
    public Integer getAboxResultRetrievalLimit() {
        return aboxResultRetrievalLimit;
    }

    /**
     * An internal limit for retrieving only this amount of query solutions.
     * Will determine mostly how big the ABox will be.
     *
     * @param aboxResultRetrievalLimit individual retrieval limit
     */
    public void setAboxResultRetrievalLimit(Integer aboxResultRetrievalLimit) {
        this.aboxResultRetrievalLimit = aboxResultRetrievalLimit;
    }

    /**
     * If this ratio is set to anything greater 0, the Concept creation allows random negation mutations on
     * the concepts.
     * The original concept will still be added, however a concept containing a Complement of a part of it might be added as well
     *
     * @return negationMutationRatio
     */
    public Double getNegationMutationRatio() {
        return negationMutationRatio;
    }

    /**
     * If this ratio is set to anything greater 0, the Concept creation allows random negation mutations on
     * the concepts.
     * The original concept will still be added, however a concept containing a Complement of a part of it might be added as well
     *
     * @param negationMutationRatio negation mutation ratio
     */
    public void setNegationMutationRatio(Double negationMutationRatio) {
        this.negationMutationRatio = negationMutationRatio;
    }


    /**
     * assures that if minimal number of examples are not less than that value.
     *
     *
     * @return strict
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * assures that if minimal number of examples are not less than that value.
     *
     * @param strict if is strict
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Sets the Query limit for positive example retrievals.
     * 
     * 0: no limit (there might still a limit if a SPARQL endpoint is used)
     *
     * @return positiveLimit
     */
    public Integer getPositiveLimit() {
        return positiveLimit;
    }

    /**
     * Sets the Query limit for positive example retrievals.
     * 
     * 0: no limit (there might still a limit if a SPARQL endpoint is used)
     *
     * @param positiveLimit positive query limit
     */
    public void setPositiveLimit(Integer positiveLimit) {
        this.positiveLimit = positiveLimit;
    }

    /**
     * Gets the Query limit for negative example retrievals.
     *
     * @return negativeLimit
     */
    public Integer getNegativeLimit() {
        return negativeLimit;
    }

    /**
     * Sets the Query limit for negative example retrievals.
     *
     * @param negativeLimit negative query limit
     */
    public void setNegativeLimit(Integer negativeLimit) {
        this.negativeLimit = negativeLimit;
    }

    /**
     * Sets the ratio how many problems will be set to train and how many to test.
     * 
     * 1 = all train, 0 all test
     *
     * @return ratio train/test
     */
    public Double getSplitContainment() {
        return splitContainment;
    }

    /**
     * Sets the ratio how many problems will be set to train and how many to test.
     * 
     * 1 = all train, 0 all test
     *
     * @param splitContainment train/test split
     */
    public void setSplitContainment(Double splitContainment) {
        this.splitContainment = splitContainment;
    }

    /**
     * Gets the namespace to use.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace to use.
     *
     * @param namespace the namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * if set true, will remove literals from Ontology.
     *
     * @return the boolean
     */
    public boolean isRemoveLiterals() {
        return removeLiterals;
    }

    /**
     * if set true, will remove literals from Ontology.
     *
     * @param removeLiterals the remove literals
     */
    public void setRemoveLiterals(boolean removeLiterals) {
        this.removeLiterals = removeLiterals;
    }

    /**
     * Sets Open World or Closed World Assumption
     *
     * @return true of is OWA, false if it is CWA
     */
    public boolean isOpenWorldAssumption() {
        return openWorldAssumption;
    }

    /**
     * Sets Open World or Closed World Assumption
     *
     * @param openWorldAssumption Open World or Closed World Assumption
     */
    public void setOpenWorldAssumption(boolean openWorldAssumption) {
        this.openWorldAssumption = openWorldAssumption;
    }

    /**
     * Gets the maximum depth for generating concepts.
     *
     * @return the max depth
     */
    public Integer getMaxDepth() {
        return maxDepth;
    }

    /**
     * Sets the maximum depth for generating concepts.
     *
     * @param maxDepth the max depth
     */
    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Gets the maximum concept length
     *
     * @return the max concept length
     */
    public Integer getMaxConceptLength() {
        return maxConceptLength;
    }

    /**
     * Sets the maximum concept length
     *
     * @param maxConceptLength the max concept length
     */
    public void setMaxConceptLength(Integer maxConceptLength) {
        this.maxConceptLength = maxConceptLength;
    }

    /**
     * Gets the minimum concept length
     *
     * @return the min concept length
     */
    public Integer getMinConceptLength() {
        return minConceptLength;
    }

    /**
     * Sets the minimum concept length.
     *
     * @param minConceptLength the min concept length
     */
    public void setMinConceptLength(Integer minConceptLength) {
        this.minConceptLength = minConceptLength;
    }

    /**
     * Allows to expand the allowed types by using direct super classes of the allowed types.
     * These will not be used to directly generate concepts, but merely allow these direct super classes
     * to be in the concept.
     *
     * @return the infer direct super classes
     */
    public Boolean getInferDirectSuperClasses() {
        return inferDirectSuperClasses;
    }

    /**
     * Allows to expand the allowed types by using direct super classes of the allowed types.
     * These will not be used to directly generate concepts, but merely allow these direct super classes
     * to be in the concept.
     *
     * @param inferDirectSuperClasses the infer direct super classes
     */
    public void setInferDirectSuperClasses(Boolean inferDirectSuperClasses) {
        this.inferDirectSuperClasses = inferDirectSuperClasses;
    }

    /**
     * Gets the amount of concepts which should be generated.
     * May be less if there are no more concepts (with enough examples
     *
     * @return the max generate concepts
     */
    public Integer getMaxGenerateConcepts() {
        return maxGenerateConcepts;
    }

    /**
     * the amount of concepts which should be generated.
     * May be less if there are no more concepts (with enough examples).
     *
     *
     * @param maxGenerateConcepts the max generate concepts
     */
    public void setMaxGenerateConcepts(Integer maxGenerateConcepts) {
        this.maxGenerateConcepts = maxGenerateConcepts;
    }


    /**
     * Gets max no of examples.
     * Every problem will have at most this amount of positive as well as negative examples.
     *
     * @return the max no of examples
     */
    public Integer getMaxNoOfExamples() {
        return maxNoOfExamples;
    }

    /**
     * Sets max no of examples.
     * Every problem will have at most this amount of positive as well as negative examples.
     *
     *
     * @param maxNoOfExamples the max no of examples
     */
    public void setMaxNoOfExamples(Integer maxNoOfExamples) {
        this.maxNoOfExamples = maxNoOfExamples;
    }

    /**
     * Gets the minimal no of examples.
     * hence keeps this amount of examples no matter what.
     *
     * 
     * If strict is set as well, all problems will have at least
     * minNoOfExamples positive as well as negative examples
     *
     * @return the min no of examples
     */
    public Integer getMinNoOfExamples() {
        return minNoOfExamples;
    }

    /**
     * Sets the minimal no of examples.
     * hence keeps this amount of examples no matter what.
     *
     * 
     * If strict is set as well, all problems will have at least
     * minNoOfExamples positive as well as negative examples
     *
     * @param minNoOfExamples the min no of examples
     */
    public void setMinNoOfExamples(Integer minNoOfExamples) {
        this.minNoOfExamples = minNoOfExamples;
    }

    /**
     * Gets the seed which will determine all random things.
     *
     * @return the seed
     */
    public Integer getSeed() {
        return seed;
    }

    /**
     * Sets the seed which will determine all random things.
     *
     * @param seed the seed
     */
    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    /**
     * Gets the allowed types.
     *
     * @return the types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * Sets the allowed types to use.
     *
     * @param types the types
     */
    public void setTypes(List<String> types) {
        this.types = types;
    }

    /**
     * Gets the percentage of positive examples the test should keep.
     *
     * @return the percentage of positive examples
     */
    public Double getPercentageOfPositiveExamples() {
        return percentageOfPositiveExamples;
    }

    /**
     * Sets the percentage of positive examples the test should keep..
     *
     * @param percentageOfPositiveExamples the percentage of positive examples
     */
    public void setPercentageOfPositiveExamples(Double percentageOfPositiveExamples) {
        this.percentageOfPositiveExamples = percentageOfPositiveExamples;
    }

    /**
     * the percentage of negative examples the test and train should keep.
     *
     * @return the percentage of negative examples
     */
    public Double getPercentageOfNegativeExamples() {
        return percentageOfNegativeExamples;
    }

    /**
     * Sets the percentage of negative examples the test and train should keep.
     *
     * @param percentageOfNegativeExamples the percentage of negative examples
     */
    public void setPercentageOfNegativeExamples(Double percentageOfNegativeExamples) {
        this.percentageOfNegativeExamples = percentageOfNegativeExamples;
    }

    /**
     * Sets the positive and negative concepts to retrieve examples from.
     *
     * @return the concepts
     */
    public List<PosNegExample> getConcepts() {
        return concepts;
    }

    /**
     * Sets the positive and negative concepts to retrieve examples from.
     *
     * @param concepts the concepts
     */
    public void setConcepts(List<PosNegExample> concepts) {
        this.concepts = concepts;
    }

    /**
     * The endpoint, either representing an RDF file or a SPARQL endpoint which includes the ABox..
     *
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * The endpoint, either representing an RDF file or a SPARQL endpoint which includes the ABox.
     *
     * @param endpoint the endpoint
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * The Ontology File containing at least the TBox
     *
     * @return the owl file
     */
    public String getOwlFile() {
        return owlFile;
    }

    /**
     * The Ontology File containing at least the TBox
     *
     * @param owlFile the owl file
     */
    public void setOwlFile(String owlFile) {
        this.owlFile = owlFile;
    }

    /**
     * prepares the configuration.
     * If negative concepts were set, convert them to ClassExpressions
     *
     * @param parser the parser containing the ontology to convert the expressions with
     */
    public void prepare(Parser parser) {
        if(concepts!=null){
            for(PosNegExample example : concepts){
                for(String negativeConcept : example.getNegatives()){
                    example.getNegativesExpr().add(parser.parseManchesterConcept(negativeConcept));
                }
            }
        }
    }
}
