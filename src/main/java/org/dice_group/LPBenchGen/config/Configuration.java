package org.dice_group.LPBenchGen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Configuration.
 *
 * @author Lixi Alié Conrads
 */
public class Configuration {

    /**
     * Load from file configuration.
     *
     * @param file the file
     * @return the configuration
     * @throws IOException the io exception
     */
    public static Configuration loadFromFile(String file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(file), Configuration.class);
    }

    /*
endpoint: http://..
owlFile: /raki/owl.owl
types:
  - Musician
  - Album
  - Actor
  - Country
maxNoOfIndividuals: 5
percentageOfPositiveExamples: 0.25
percentageOfNegativeExamples: 0.25
concepts:
  - positive: president and female
    negatives:
      - president and male
      - female and not president
  - positive: musician and actor
    negatives: ...
seed:
*/


    @JsonProperty(required = false)
    private List<String> types = new ArrayList<>();
    @JsonProperty(required = false, defaultValue = "0")
    private Integer maxNoOfIndividuals=0;
    @JsonProperty(required = false, defaultValue = "0.5")
    private Double percentageOfPositiveExamples=0.5;
    @JsonProperty(required = false, defaultValue = "0.5")
    private Double percentageOfNegativeExamples=0.5;
    @JsonProperty(required = false, defaultValue = "1")
    private Integer seed=1;
    @JsonProperty(required = false, defaultValue = "200")
    private Integer maxIndividualsPerExampleConcept=200;
    @JsonProperty(required = false, defaultValue = "30")
    private Integer maxNoOfExamples=30;
    @JsonProperty(required = false, defaultValue = "5")
    private Integer minNoOfExamples=5;
    @JsonProperty(required = false)
    private List<PosNegExample> concepts;
    @JsonProperty(required = false, defaultValue = "20")
    private Integer maxGenerateConcepts=20;
    @JsonProperty(required = false, defaultValue = "2")
    private Integer maxDepth=2;
    @JsonProperty(required = false, defaultValue = "10")
    private Integer maxConceptLength=10;
    @JsonProperty(required = false, defaultValue = "4")
    private Integer minConceptLength=4;
    @JsonProperty(required = false, defaultValue = "true")
    private Boolean inferDirectSuperClasses=true;
    @JsonProperty(required = true)
    private String endpoint;
    @JsonProperty(required = true)
    private String owlFile;
    @JsonProperty(required = false)
    private boolean endpointInfersRules=false;
    @JsonProperty(required = false)
    private boolean removeLiterals=false;
    @JsonProperty(required = false)
    private String namespace;

    /**
     * Gets namespace.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets namespace.
     *
     * @param namespace the namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Is remove literals boolean.
     *
     * @return the boolean
     */
    public boolean isRemoveLiterals() {
        return removeLiterals;
    }

    /**
     * Sets remove literals.
     *
     * @param removeLiterals the remove literals
     */
    public void setRemoveLiterals(boolean removeLiterals) {
        this.removeLiterals = removeLiterals;
    }

    /**
     * Is endpoint infers rules boolean.
     *
     * @return the boolean
     */
    public boolean isEndpointInfersRules() {
        return endpointInfersRules;
    }

    /**
     * Sets endpoint infers rules.
     *
     * @param endpointInfersRules the endpoint infers rules
     */
    public void setEndpointInfersRules(boolean endpointInfersRules) {
        this.endpointInfersRules = endpointInfersRules;
    }

    /**
     * Gets max depth.
     *
     * @return the max depth
     */
    public Integer getMaxDepth() {
        return maxDepth;
    }

    /**
     * Sets max depth.
     *
     * @param maxDepth the max depth
     */
    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Gets max concept length.
     *
     * @return the max concept length
     */
    public Integer getMaxConceptLength() {
        return maxConceptLength;
    }

    /**
     * Sets max concept length.
     *
     * @param maxConceptLength the max concept length
     */
    public void setMaxConceptLength(Integer maxConceptLength) {
        this.maxConceptLength = maxConceptLength;
    }

    /**
     * Gets min concept length.
     *
     * @return the min concept length
     */
    public Integer getMinConceptLength() {
        return minConceptLength;
    }

    /**
     * Sets min concept length.
     *
     * @param minConceptLength the min concept length
     */
    public void setMinConceptLength(Integer minConceptLength) {
        this.minConceptLength = minConceptLength;
    }

    /**
     * Gets infer direct super classes.
     *
     * @return the infer direct super classes
     */
    public Boolean getInferDirectSuperClasses() {
        return inferDirectSuperClasses;
    }

    /**
     * Sets infer direct super classes.
     *
     * @param inferDirectSuperClasses the infer direct super classes
     */
    public void setInferDirectSuperClasses(Boolean inferDirectSuperClasses) {
        this.inferDirectSuperClasses = inferDirectSuperClasses;
    }

    /**
     * Gets max generate concepts.
     *
     * @return the max generate concepts
     */
    public Integer getMaxGenerateConcepts() {
        return maxGenerateConcepts;
    }

    /**
     * Sets max generate concepts.
     *
     * @param maxGenerateConcepts the max generate concepts
     */
    public void setMaxGenerateConcepts(Integer maxGenerateConcepts) {
        this.maxGenerateConcepts = maxGenerateConcepts;
    }

    /**
     * Gets max individuals per example concept.
     *
     * @return the max individuals per example concept
     */
    public Integer getMaxIndividualsPerExampleConcept() {
        return maxIndividualsPerExampleConcept;
    }

    /**
     * Sets max individuals per example concept.
     *
     * @param maxIndividualsPerExampleConcept the max individuals per example concept
     */
    public void setMaxIndividualsPerExampleConcept(Integer maxIndividualsPerExampleConcept) {
        this.maxIndividualsPerExampleConcept = maxIndividualsPerExampleConcept;
    }

    /**
     * Gets max no of examples.
     *
     * @return the max no of examples
     */
    public Integer getMaxNoOfExamples() {
        return maxNoOfExamples;
    }

    /**
     * Sets max no of examples.
     *
     * @param maxNoOfExamples the max no of examples
     */
    public void setMaxNoOfExamples(Integer maxNoOfExamples) {
        this.maxNoOfExamples = maxNoOfExamples;
    }

    /**
     * Gets min no of examples.
     *
     * @return the min no of examples
     */
    public Integer getMinNoOfExamples() {
        return minNoOfExamples;
    }

    /**
     * Sets min no of examples.
     *
     * @param minNoOfExamples the min no of examples
     */
    public void setMinNoOfExamples(Integer minNoOfExamples) {
        this.minNoOfExamples = minNoOfExamples;
    }

    /**
     * Gets seed.
     *
     * @return the seed
     */
    public Integer getSeed() {
        return seed;
    }

    /**
     * Sets seed.
     *
     * @param seed the seed
     */
    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    /**
     * Gets types.
     *
     * @return the types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * Sets types.
     *
     * @param types the types
     */
    public void setTypes(List<String> types) {
        this.types = types;
    }

    /**
     * Gets max no of individuals.
     *
     * @return the max no of individuals
     */
    public Integer getMaxNoOfIndividuals() {
        return maxNoOfIndividuals;
    }

    /**
     * Sets max no of individuals.
     *
     * @param maxNoOfIndividuals the max no of individuals
     */
    public void setMaxNoOfIndividuals(Integer maxNoOfIndividuals) {
        this.maxNoOfIndividuals = maxNoOfIndividuals;
    }

    /**
     * Gets percentage of positive examples.
     *
     * @return the percentage of positive examples
     */
    public Double getPercentageOfPositiveExamples() {
        return percentageOfPositiveExamples;
    }

    /**
     * Sets percentage of positive examples.
     *
     * @param percentageOfPositiveExamples the percentage of positive examples
     */
    public void setPercentageOfPositiveExamples(Double percentageOfPositiveExamples) {
        this.percentageOfPositiveExamples = percentageOfPositiveExamples;
    }

    /**
     * Gets percentage of negative examples.
     *
     * @return the percentage of negative examples
     */
    public Double getPercentageOfNegativeExamples() {
        return percentageOfNegativeExamples;
    }

    /**
     * Sets percentage of negative examples.
     *
     * @param percentageOfNegativeExamples the percentage of negative examples
     */
    public void setPercentageOfNegativeExamples(Double percentageOfNegativeExamples) {
        this.percentageOfNegativeExamples = percentageOfNegativeExamples;
    }

    /**
     * Gets concepts.
     *
     * @return the concepts
     */
    public List<PosNegExample> getConcepts() {
        return concepts;
    }

    /**
     * Sets concepts.
     *
     * @param concepts the concepts
     */
    public void setConcepts(List<PosNegExample> concepts) {
        this.concepts = concepts;
    }

    /**
     * Gets endpoint.
     *
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sets endpoint.
     *
     * @param endpoint the endpoint
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Gets owl file.
     *
     * @return the owl file
     */
    public String getOwlFile() {
        return owlFile;
    }

    /**
     * Sets owl file.
     *
     * @param owlFile the owl file
     */
    public void setOwlFile(String owlFile) {
        this.owlFile = owlFile;
    }
}
