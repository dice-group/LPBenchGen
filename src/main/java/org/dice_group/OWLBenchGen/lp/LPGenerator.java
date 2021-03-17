package org.dice_group.OWLBenchGen.lp;

import org.dice_group.OWLBenchGen.config.Configuration;
import org.dice_group.OWLBenchGen.config.PosNegExample;
import org.dice_group.OWLBenchGen.dl.Parser;
import org.dice_group.OWLBenchGen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class LPGenerator {

    private IndividualRetriever retriever;
    private long seed=0;
    private int minExamples;
    private int maxExamples;
    private Parser parser;


    public void createBenchmark(String configFile, String name) throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile(configFile);
        parser = new Parser(conf.getOwlFile());
        retriever = new IndividualRetriever(conf.getEndpoint());
        seed= conf.getSeed();
        minExamples=conf.getMinNoOfExamples();
        maxExamples=conf.getMaxNoOfExamples();
        Collection<LPProblem> problems = new ArrayList<LPProblem>();
        for(PosNegExample concept : conf.getConcepts()){
            problems.add(generateLPProblem(concept, parser));
        }
        //for all individuals in LP problem add them

        Collection<String> individuals = getIndividuals(problems, conf.getTypes(), conf.getMaxNoOfIndividuals(), conf.getPercentageOfNegativeExamples(), conf.getPercentageOfPositiveExamples());
        for(String individual : individuals){
            addIndividuals(parser.getOntology(), createIndividual(individual, conf.getTypes()));
        }
        saveOntology(name+"-ontology.owl", parser.getOntology());
        saveLPProblem(name+"-lp.json", problems);
    }

    private Collection<String> getIndividuals(Collection<LPProblem> problems, List<String> types, Integer maxNoOfIndividuals,Double percentageOfNegativeExamples, Double percentageOfPositiveExamples) {
        Collection<String> ret = new TreeSet<String>();

        for(String type : types){
            List<String> typeIndividuals = retriever.retrieveIndividualsForType(type);
            ret.addAll(getRandom(typeIndividuals, maxNoOfIndividuals));
        }
        for(LPProblem problem : problems){
            List<String> removedPositives = keepPercentage(problem.positives, percentageOfPositiveExamples);
            List<String> removedNegatives = keepPercentage(problem.negatives, percentageOfNegativeExamples);
            ret.addAll(problem.positives);
            ret.addAll(problem.negatives);
            ret.addAll(getRandom(removedNegatives, maxNoOfIndividuals));
            ret.addAll(getRandom(removedPositives, maxNoOfIndividuals));
        }
        return ret;
    }

    private Collection<String> getRandom(List<String> list, Integer max) {
        int maxNoOfIndividuals = Math.min(max, list.size());
        Set<Integer> indexes = new HashSet<Integer>();
        Random rand = new Random(seed);
        while(indexes.size()<maxNoOfIndividuals) {
            indexes.add(rand.nextInt(list.size()));
        }
        Collection<String> ret = new HashSet<String>();
        for(Integer index : indexes){
            ret.add(list.get(index));
        }
        return ret;
    }

    private List<String> keepPercentage(Collection<String> removeFrom, Double percentage) {
        List<String> removed = new ArrayList<String>();
        Double max = removeFrom.size()*percentage;
        if(max<minExamples){
            max=minExamples*1.0;
        }
        if(max>maxExamples){
            max=maxExamples*1.0;
        }
        Collection<String> keep = getRandom(new ArrayList(removeFrom), max.intValue());
        removeFrom.clear();
        removeFrom.addAll(keep);
        return removed;
    }

    private LPProblem generateLPProblem(PosNegExample concept, Parser parser) {
        LPProblem problem = new LPProblem();
        problem.goldStandardConcept=concept.getPositive();

        problem.positives.addAll(retriever.retrieveIndividualsForConcept(parser.parseManchesterConcept(concept.getPositive())));
        for(String negative : concept.getNegatives()){
            problem.negatives.addAll(retriever.retrieveIndividualsForConcept(parser.parseManchesterConcept(negative)));
        }
        return problem;
    }

    public Collection<OWLAxiom> createIndividual(String uri, Collection<String> typesAllowed){
        OWLDataFactory factory = new OWLDataFactoryImpl();
        Collection<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
        //1. retrieve all types (which are also in the config) for uri as subject
        Collection<String> types = retriever.retrieveTypesForIndividual(uri);
        Collection<String> disallowed = new HashSet<String>();
        for(String type : types){
            if(!typesAllowed.contains(type)){
                disallowed.add(type);
            }
        }
        types.removeAll(disallowed);
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(uri));
        //3. create with OWLDataFactory assertions for all retrieved types
        for(String type : types){
            OWLClassExpression expr = factory.getOWLClass(IRI.create(type));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(expr, individual);
            axioms.add(axiom);
        }
        return axioms;
    }

    public void addIndividuals(OWLOntology ontology, Collection<OWLAxiom> individuals){
        for(OWLAxiom axiom : individuals){
            ontology.addAxiom(axiom);
        }
    }

    public void saveOntology(String output, OWLOntology ontology){
        try(FileOutputStream fos = new FileOutputStream(output))   {
            ontology.saveOntology(fos);
        } catch (IOException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }

    }

    public void saveLPProblem(String output, Collection<LPProblem> problems){
        try(PrintWriter pw = new PrintWriter(output)){
            // print end
            pw.print("[\n");
            int count=0;
            for(LPProblem problem: problems ) {
                pw.print("\t{\n\t\"concept\": \"");
                pw.print(problem.goldStandardConcept);
                pw.print("\",\n\t\"positives\": [\n");
                writeCollection(problem.positives, pw);
                pw.print("\t\n],\n\t\"negatives\": [\n");
                writeCollection(problem.negatives, pw);
                pw.print("\t\n]\n\t}");
                count++;
                if(count<problems.size()){
                    pw.print(",\n");
                }
            }
            pw.print("\n]");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeCollection(Collection<String> collection, PrintWriter pw) {
        int count=0;
        for(String uri : collection){
            pw.print("\t\t\"");
            pw.print(uri);
            pw.print("\"");
            count++;
            if(count<collection.size()){
                pw.print(",\n");
            }

        }
    }
}
