package org.dice_group.lpbenchgen;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class EvaluationTest {

    @Test
    public void checkFMeausureCalculation(){
        double[] f1 = Evaluation.f1measure(0,0,0);
        assertEquals(1, f1[0], 0);
        assertEquals(1, f1[1], 0);
        assertEquals(1, f1[2], 0);
        f1 = Evaluation.f1measure(0,10,2);
        assertEquals(0, f1[0], 0);
        assertEquals(0, f1[1], 0);
        assertEquals(0, f1[2], 0);

        f1 = Evaluation.f1measure(0,10,0);
        assertEquals(0, f1[0], 0);
        assertEquals(0, f1[1], 0);
        assertEquals(0, f1[2], 0);
        f1 = Evaluation.f1measure(0,0,1);
        assertEquals(0, f1[0], 0);
        assertEquals(0, f1[1], 0);
        assertEquals(0, f1[2], 0);

        f1 = Evaluation.f1measure(5,0,0);
        assertEquals(1, f1[0], 0);
        assertEquals(1, f1[1], 0);
        assertEquals(1, f1[2], 0);

        f1 = Evaluation.f1measure(3,3,3);
        assertEquals(0.5, f1[0], 0);
        assertEquals(0.5, f1[1], 0);
        assertEquals(0.5, f1[2], 0);

        f1 = Evaluation.f1measure(3,0,3);
        assertEquals(1/1.5, f1[0], 0);
        assertEquals(0.5, f1[1], 0);
        assertEquals(1, f1[2], 0);

        f1 = Evaluation.f1measure(3,3,0);
        assertEquals(1/1.5, f1[0], 0);
        assertEquals(1, f1[1], 0);
        assertEquals(0.5, f1[2], 0);
    }

    @Test
    public void checkEvaluationLists(){
        List<String> gold = Lists.newArrayList("A", "B", "C");
        List<String> system = Lists.newArrayList("A", "B", "C");
        double[] test = Evaluation.evaluate(gold, system);
        assertEquals(3, test[0],0);
        assertEquals(0, test[1],0);
        assertEquals(0, test[2],0);
        assertEquals(1, test[3],0);
        assertEquals(1, test[4],0);
        assertEquals(1, test[5],0);

        system = Lists.newArrayList("D", "E", "F");
        test = Evaluation.evaluate(gold, system);
        assertEquals(0, test[0],0);
        assertEquals(3, test[1],0);
        assertEquals(3, test[2],0);
        assertEquals(0, test[3],0);
        assertEquals(0, test[4],0);
        assertEquals(0, test[5],0);

        system = Lists.newArrayList("A", "B", "C", "D", "E", "F");
        test = Evaluation.evaluate(gold, system);
        assertEquals(3, test[0],0);
        assertEquals(3, test[1],0);
        assertEquals(0, test[2],0);
        assertEquals(1/1.5, test[3],0);
        assertEquals(1, test[4],0);
        assertEquals(0.5, test[5],0);
    }

    @Test
    public void checkFullEvaluateDefault() throws IOException {
        //Test default
        String output = UUID.randomUUID().toString()+".tsv";
        Evaluation.main(new String[]{"src/test/resources/eval/gold.ttl",
                "src/test/resources/eval/test.ttl",
                "src/test/resources/eval/answers-includes.ttl",
                output
        });
        List<String> answers = FileUtils.readLines(new File(output), Charset.defaultCharset());
        List<String> expected = FileUtils.readLines(new File("src/test/resources/eval/expected.tsv"), Charset.defaultCharset());
        assertEquals(expected.size(), answers.size());
        answers.removeAll(expected);
        assertEquals(0, answers.size());
        new File(output).delete();
    }

    @Test
    public void checkFullEvaluateIncludes() throws IOException {
        String output = UUID.randomUUID().toString()+".tsv";
        Evaluation.main(new String[]{"--includes-format",
                "src/test/resources/eval/gold.ttl",
                "src/test/resources/eval/test.ttl",
                "src/test/resources/eval/answers-includes.ttl",
                output
        });
        List<String> answers = FileUtils.readLines(new File(output), Charset.defaultCharset());
        List<String> expected = FileUtils.readLines(new File("src/test/resources/eval/expected.tsv"), Charset.defaultCharset());
        assertEquals(expected.size(), answers.size());
        answers.removeAll(expected);
        assertEquals(0, answers.size());
        new File(output).delete();
    }

    @Test
    public void checkFullEvaluatePertain() throws IOException {
        String output = UUID.randomUUID().toString() + ".tsv";
        Evaluation.main(new String[]{"--pertain-format",
                "src/test/resources/eval/gold.ttl",
                "src/test/resources/eval/test.ttl",
                "src/test/resources/eval/answers-pertain.ttl",
                output
        });
        List<String> answers = FileUtils.readLines(new File(output), Charset.defaultCharset());
        List<String> expected = FileUtils.readLines(new File("src/test/resources/eval/expected.tsv"), Charset.defaultCharset());
        assertEquals(expected.size(), answers.size());
        answers.removeAll(expected);
        assertEquals(0, answers.size());
        new File(output).delete();
    }

}
