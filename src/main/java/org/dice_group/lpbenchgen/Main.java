package org.dice_group.lpbenchgen;


import com.google.common.collect.Lists;
import org.apache.jena.query.ARQ;
import org.dice_group.lpbenchgen.lp.LPGenerator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.util.List;

/**
 * The type Main.
 *
 * @author Lixi Alié Conrads
 */
public class Main {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws OWLOntologyCreationException the owl ontology creation exception
     * @throws IOException                  the io exception
     */
    public static void main(String[] args) throws OWLOntologyCreationException, IOException {
        if(args.length!=7){
            printHelp();
        }
        else {
            //somehow we need to do this manually otherwise the jar will not do it for weird reasons I do not understand.
            ARQ.init();
            List<String> arguments = Lists.newArrayList(args);
            String name="";
            String config="";
            String format="";
            if(!arguments.contains("--format") || !arguments.contains("--name") || ! arguments.contains("--config") || !(arguments.contains("--concept") || arguments.contains("--containment"))){
                printHelp();
                System.exit(1);
            }
            name = arguments.get(arguments.indexOf("--name")+1);
            config = arguments.get(arguments.indexOf("--config")+1);
            String benchmarkType = arguments.contains("--concept")?"concept":"containment";
            format = arguments.get(arguments.indexOf("--format")+1).toLowerCase();
            new LPGenerator().createBenchmark(config, name, benchmarkType, format);
        }
    }

    /**
     * Print help.
     */
    public static void printHelp(){
        String help =" lpbenchgen --format [json|rdf] --config <CONFIG.YML> --name <BENCHMARK-NAME> [--concept|--containment]\n" +
                "\n\tCONCEPT:" +
                "\n\twill create learning problems and an ontology " +
                "\n\tbased upon a SPARQL endpoint and a base ontology" +
                "\n\tfor each concept in <CONFIG.YML> a learning problem will be created" +
                "\n\tand the corresponding Individuals will be added to the ontology" +
                "\n\tfurther on a certain amount of random Individuals will be added as well which will be retrieved using the types" +
                "\n\tstated in <CONFIG.YML>" +
                "\n\n\tOutput at <BENCHMARK-NAME>-lp.json and <BENCHMARK-NAME>-ontology.owl" +
                "\n\n\n\tCONTAINMENT:" +
                "\n\twill create learning problems and split them into trainingsdata as well as two additional sets of test data and gold standard" +
                "\n\n\tOutput at <BENCHMARK-NAME>-(train/test).(json/ttl) and <BENCHMARK-NAME>-test-goldstd.(json/ttl)";
        System.out.println(help);
    }

}