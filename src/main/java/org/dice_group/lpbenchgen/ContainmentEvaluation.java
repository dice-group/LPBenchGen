package org.dice_group.lpbenchgen;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.lp.LPGenerator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContainmentEvaluation {

    public static void main(String[] args) throws FileNotFoundException {
        if(args.length<4 || args.length>5){
            printHelp();
        }
        else{
            boolean isPertainFormat=false;
            int start=0;
            if(args.length==5){
                start++;
                if(args[0].equals("--pertain-format")){
                    isPertainFormat=true;
                }
                else if(args[0].equals("--includes-format")){
                    isPertainFormat=false;
                }
                else{
                    printHelp();
                }
            }
            Model gold = ModelFactory.createDefaultModel();
            gold.read(new FileReader(args[start]), null, "TTL");
            Model test = ModelFactory.createDefaultModel();
            test.read(new FileReader(args[start+1]), null, "TTL");
            Model answers = ModelFactory.createDefaultModel();
            answers.read(new FileReader(args[start+2]), null, "TTL");
            evaluate(gold, test, answers, args[start+3], isPertainFormat);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: ContainmentEvaluation (--pertain-format | --includes-format) gold.ttl test.ttl answers.ttl OUTPUT_REPORT_FILE");
        System.out.println("\n\t--pertain-format  -  answers.ttl is in pertain format");
        System.out.println("\t--includes-format (DEFAULT)  -  answers.ttl is in includes format");
        System.out.println("\n\tPertain Format: ");
        System.out.println("\t\t@prefix lpres:<https://lpbenchgen.org/resource/>");
        System.out.println("\t\t@prefix lpprop:<https://lpbenchgen.org/property/>\n");
        System.out.println("\t\tlpres:result_1 lpprop:pertainsTo lpres:lp_1;\n" +
                "\t\t\tlpprop:resource test:Individual2;\n" +
                "\t\t\tlpprop:resource test:Individual1;\n" +
                "\t\t\tlpprop:belongsToLP true.\n" +
                "\n\t\tlpres:result_2 lpprop:pertainsTo lpres:lp_1;\n" +
                "\t\t\tlpprop:resource test:Individual9;\n" +
                "\t\t\tlpprop:belongsToLP true.");
        System.out.println("\n\tIncludes Format: ");
        System.out.println("\t\t@prefix lpres:<https://lpbenchgen.org/resource/>");
        System.out.println("\t\t@prefix lpprop:<https://lpbenchgen.org/property/>\n");
        System.out.println("\t\tlpres:lp_1 lpprop:includesResource test:Individual1, test:Individual9, test:Individual2 .");
        System.exit(1);
    }

    public static void evaluate(Model goldStd, Model test, Model answerModel, String out, boolean pertainFormat) throws FileNotFoundException {
        try(PrintWriter pw = new PrintWriter(out)) {
            double f1=0.0, recall=0.0, precision=0.0;
            int tp=0, fp=0, fn=0;

            pw.println("id\ttp\tfp\tfn\tf1\trecall\tprecision");
            List<Resource> problems = new ArrayList<Resource>();
            goldStd.listStatements(null, RDF.type, LPGenerator.LEARNING_PROBLEM_CLASS).forEachRemaining(problem -> {
                problems.add(problem.getSubject());
            });
            for (Resource problem : problems) {
                List<String> gold = new ArrayList<String>();
                goldStd.listStatements(problem, LPGenerator.RDF_PROPERTY_INCLUDE, (RDFNode) null).forEachRemaining(triple -> {
                    gold.add(triple.getObject().asResource().toString());
                });
                List<String> remove = new ArrayList<String>();
                test.listStatements(problem, LPGenerator.RDF_PROPERTY_INCLUDE, (RDFNode) null).forEachRemaining(triple -> {
                    remove.add(triple.getObject().asResource().toString());
                });
                gold.removeAll(remove);

                List<String> answers = new ArrayList<String>();
                if(pertainFormat){
                    addAnswersPertain(problem, answers, answerModel);
                }
                else {
                    addAnswers(problem, answers, answerModel);
                }
                answers.removeAll(remove);

                double[] values = evaluate(gold, answers);
                tp+=values[0];
                fp+=values[1];
                fn+=values[2];
                f1+=values[3];
                recall+=values[4];
                precision+=values[5];
                pw.print(problem.getURI().replace(LPGenerator.RDF_PREFIX+"resource/", ""));
                for(double val : values){
                    pw.print("\t");
                    pw.print(val);
                }
                pw.println();
            }
            double[] micro = fmeasure(tp,fp,fn);
            double[] macro = new double[]{f1*1.0/problems.size(), recall*1.0/problems.size(), precision*1.0/problems.size()};
            pw.println();
            pw.println("Micro F1\t"+micro[0]);
            pw.println("Micro Recall\t"+micro[1]);
            pw.println("Micro Precision\t"+micro[2]);
            pw.println("Macro F1\t"+macro[0]);
            pw.println("Macro Recall\t"+macro[1]);
            pw.println("Macro Precision\t"+macro[2]);
        }
    }


    private static void addAnswersPertain(Resource problem, List<String> answers, Model answerModel) {
        String queryStr = "PREFIX lpres: <https://lpbenchgen.org/resource/> PREFIX lpprop: <https://lpbenchgen.org/property/> SELECT ?res {?s lpprop:pertainsTo <"+problem.getURI()+">; " +
                "   lpprop:belongsToLP true ; " +
                "   lpprop:resource ?res .}";
        QueryExecution exec = QueryExecutionFactory.create(QueryFactory.create(queryStr), answerModel);
        ResultSet res = exec.execSelect();
        String var  =res.getResultVars().get(0);
        while(res.hasNext()){
            answers.add(res.next().get(var).asResource().getURI());
        }
    }

    private static void addAnswers(Resource problem, List<String> answers, Model answerModel) {
        answerModel.listStatements(problem, LPGenerator.RDF_PROPERTY_INCLUDE, (RDFNode) null).forEachRemaining(triple -> {
            answers.add(triple.getObject().asResource().toString());
        });
    }

    private static double[] evaluate(List<String> gold, List<String> answers) {
        int tp = 0;
        int fp = 0;
        int fn = 0;
        double f1 = 0.0;
        double recall=0.0;
        double precision=0.0;
        for(String answer : answers){
            if(gold.contains(answer)){
                tp++;
            }
            else{
                fp++;
            }
        }
        fn = gold.size()-tp;
        double[] vals = fmeasure(tp, fp, fn);
        f1 = vals[0];
        recall = vals[1];
        precision = vals[2];
        return new double[]{tp,fp,fn,f1,recall,precision};
    }

    private static double[] fmeasure(int tp, int fp, int fn) {
        //if all are 0
        if(tp==0&&fp==0&&fn==0){
            return new double[]{1,1,1};
        }
        if(tp==0&&(fp!=0 || fn !=0)){
            return new double[]{0,0,0};
        }
        double recall=tp*1.0/(tp+fn);
        double precision=tp*1.0/(tp+fp);
        double f1 = 2*precision*recall/(precision+recall);
        return new double[]{f1,recall,precision};
    }
}
