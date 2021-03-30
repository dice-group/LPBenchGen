package org.dice_group.LPBenchGen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Configuration {

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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isRemoveLiterals() {
        return removeLiterals;
    }

    public void setRemoveLiterals(boolean removeLiterals) {
        this.removeLiterals = removeLiterals;
    }

    public boolean isEndpointInfersRules() {
        return endpointInfersRules;
    }

    public void setEndpointInfersRules(boolean endpointInfersRules) {
        this.endpointInfersRules = endpointInfersRules;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Integer getMaxConceptLength() {
        return maxConceptLength;
    }

    public void setMaxConceptLength(Integer maxConceptLength) {
        this.maxConceptLength = maxConceptLength;
    }

    public Integer getMinConceptLength() {
        return minConceptLength;
    }

    public void setMinConceptLength(Integer minConceptLength) {
        this.minConceptLength = minConceptLength;
    }

    public Boolean getInferDirectSuperClasses() {
        return inferDirectSuperClasses;
    }

    public void setInferDirectSuperClasses(Boolean inferDirectSuperClasses) {
        this.inferDirectSuperClasses = inferDirectSuperClasses;
    }

    public Integer getMaxGenerateConcepts() {
        return maxGenerateConcepts;
    }

    public void setMaxGenerateConcepts(Integer maxGenerateConcepts) {
        this.maxGenerateConcepts = maxGenerateConcepts;
    }

    public Integer getMaxIndividualsPerExampleConcept() {
        return maxIndividualsPerExampleConcept;
    }

    public void setMaxIndividualsPerExampleConcept(Integer maxIndividualsPerExampleConcept) {
        this.maxIndividualsPerExampleConcept = maxIndividualsPerExampleConcept;
    }

    public Integer getMaxNoOfExamples() {
        return maxNoOfExamples;
    }

    public void setMaxNoOfExamples(Integer maxNoOfExamples) {
        this.maxNoOfExamples = maxNoOfExamples;
    }

    public Integer getMinNoOfExamples() {
        return minNoOfExamples;
    }

    public void setMinNoOfExamples(Integer minNoOfExamples) {
        this.minNoOfExamples = minNoOfExamples;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Integer getMaxNoOfIndividuals() {
        return maxNoOfIndividuals;
    }

    public void setMaxNoOfIndividuals(Integer maxNoOfIndividuals) {
        this.maxNoOfIndividuals = maxNoOfIndividuals;
    }

    public Double getPercentageOfPositiveExamples() {
        return percentageOfPositiveExamples;
    }

    public void setPercentageOfPositiveExamples(Double percentageOfPositiveExamples) {
        this.percentageOfPositiveExamples = percentageOfPositiveExamples;
    }

    public Double getPercentageOfNegativeExamples() {
        return percentageOfNegativeExamples;
    }

    public void setPercentageOfNegativeExamples(Double percentageOfNegativeExamples) {
        this.percentageOfNegativeExamples = percentageOfNegativeExamples;
    }

    public List<PosNegExample> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<PosNegExample> concepts) {
        this.concepts = concepts;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getOwlFile() {
        return owlFile;
    }

    public void setOwlFile(String owlFile) {
        this.owlFile = owlFile;
    }
}
