package org.dice_group.LPBenchGen;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.dice_group.LPBenchGen.lp.LPGenerator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws OWLOntologyCreationException, IOException {
        if(args.length!=4){
            printHelp();
        }
        else {
            String name="";
            String config="";
            if(args[0].equals("--name")){
                name=args[1];
            }else if(args[2].equals("--name")){
                name=args[3];
            }
            else{
                printHelp();
                System.exit(1);
            }
            if(args[0].equals("--config")){
                config=args[1];
            }else if(args[2].equals("--config")){
                config=args[3];
            }
            else{
                printHelp();
                System.exit(1);
            }
            new LPGenerator().createBenchmark(config, name);
        }
    }

    public static void printHelp(){
        String help =" obscene --config <CONFIG.YML> --name <BENCHMARK-NAME> \n" +
                "\n\twill create learning problems and an ontology " +
                "\n\tbased upon a SPARQL endpoint and a base ontology" +
                "\n\tfor each concept in <CONFIG.YML> a learning problem will be created" +
                "\n\tand the corresponding Individuals will be added to the ontology" +
                "\n\tfurther on a certain amount of random Individuals will be added as well which will be retrieved using the types" +
                "\n\tstated in <CONFIG.YML>" +
                "\n\n\tOutput at <BENCHMARK-NAME>-lp.json and <BENCHMARK-NAME>-ontology.owl";
    }

}