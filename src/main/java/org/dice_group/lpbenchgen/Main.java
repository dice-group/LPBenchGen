package org.dice_group.lpbenchgen;


import com.google.common.collect.Lists;
import org.apache.jena.query.ARQ;
import org.dice_group.lpbenchgen.lp.LPBenchmark;
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
        if(args.length<6 || args.length>7){
            printHelp();
        }
        else {
            //somehow we need to do this manually otherwise the jar will not do it for weird reasons I do not understand.
            ARQ.init();
            List<String> arguments = Lists.newArrayList(args);
            String name="";
            String config="";
            String format="";
            if(!arguments.contains("--format") || !arguments.contains("--name") || ! arguments.contains("--config")){
                printHelp();
                System.exit(1);
            }
            name = arguments.get(arguments.indexOf("--name")+1);
            config = arguments.get(arguments.indexOf("--config")+1);
            boolean generateABox = arguments.contains("--generate-abox");
            format = arguments.get(arguments.indexOf("--format")+1).toLowerCase();
            LPGenerator generator = new LPGenerator();
            LPBenchmark benchmark = generator.createBenchmark(config, generateABox);
            generator.saveLPBenchmark(benchmark, name, format);
        }
    }

    /**
     * Print help.
     */
    public static void printHelp(){
        String help =" lpbenchgen --format [json|rdf] --config <CONFIG.YML> --name <BENCHMARK-NAME> [--generate-abox]\n" +
                "\n\n\n" +
                "\n\twill create learning problems from an ABox and TBox and split them into trainings data as well as two additional sets of test data and gold standard" +
                "\n\tif --generate-abox : will create a minimal ABox and saves that at <BENCHMARK-NAME>-ontology.ttl" +
                "\n\n\tOutput at <BENCHMARK-NAME>-(train/test).(json/ttl) and <BENCHMARK-NAME>-test-goldstd.(json/ttl)";
        System.out.println(help);
    }

}