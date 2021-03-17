package org.dice_group.LPBenchGen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
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


    @JsonProperty(required = true)
    private List<String> types;
    @JsonProperty(required = false, defaultValue = "0")
    private Integer maxNoOfIndividuals;
    @JsonProperty(required = false, defaultValue = "0.5")
    private Double percentageOfPositiveExamples;
    @JsonProperty(required = false, defaultValue = "0.5")
    private Double percentageOfNegativeExamples;
    @JsonProperty(required = false, defaultValue = "1")
    private Integer seed;

    @JsonProperty(required = false, defaultValue = "100")
    private Integer maxNoOfExamples;
    @JsonProperty(required = false, defaultValue = "5")
    private Integer minNoOfExamples;
    @JsonProperty(required = true)
    private List<PosNegExample> concepts;
    @JsonProperty(required = true)
    private String endpoint;
    @JsonProperty(required = true)
    private String owlFile;

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
