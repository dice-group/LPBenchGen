# About

LPBenchGen is an OWL explainable structural learning problem Benchmark Generator.

Explainable AI is becoming more important and more and more systems to tackle that problem are developed. 
However there is a lack of benchmark datasets. 

LPBenchGen tries to tackle that problem by providing a tool that can generate benchmarks using a TBox and an ABox.

The ABox can either be behind a SPARQL endpoint or in an RDF file. 
Using an RDF file allows an Open World Assumption. This is restricted in SPARQL, which generally allows only a Closed World Assumption.

## What is a Learning Problem in this context?

A learning problem is a set of positive examples and negative examples, whereas one example is an Individual contained in the ABox.

A system then may be benchmark to find the best concept/class expression or try to find all other positive Individuals .

## How does it work?

LPBenchGen will create a lot of theoretically class expression based upon the TBox and checks if these have enough solutions using the ABox.
The user can set parameters allowing longer and more complex as well as smaller and easier expressions, how many of these should be created and a lot more.

Using these expressions LPBenchGen creates positive examples for one learning problem.
However it is not straight forward to retrieve negative examples.

For example if we have a big diverse Database and have a concept such as `Car and (hasEngine some Engine)`:
A negative Example such as `Elefant` wouldn't be useful at all. 
What would be useful is something like `Motorcycle-1` as it has an engine, but is not a car.

Hence LPBenchGen creates a few class expression based upon the positive class expressions in that matter.
These basically negate some parts of the original expression and hence providing usefull negative examples.


Afterwards these concepts will be used to retrieve positive and negative examples and thus creating the learning problem. 

A benchmark in this context is a set of such learning problems. 


For a detailed description please have a look at Underlying-Model


## What does LPBenchGen generate?

LPBenchGen generates a training benchmark set, a test benchmark set and  a gold standard for the test benchmark.


## My ABox is huge, no current system can work with that!

No worries. LPBenchGen can create a small ABox fitting the problems in the benchmark. 

## How do I evaluate my system?

If you generated your benchmark accordingly (see Usage)
LPBenchGen provides a small evaluation. Simply use the test benchmark, the gold standard, and the actual system answers and evaluate. See Usage for the correct format and eval.

## How to download

Download the latest release jar file at [https://github.com/dice-group/LPBenchGen/releases/latest](https://github.com/dice-group/LPBenchGen/releases/latest)

See Getting-Started for how to execute the generation.

## Where can I find the code? 

The code is open source at [https://github.com/dice-group/LPBenchGen](https://github.com/dice-group/LPBenchGen) and you can code with us if you want to :)

## Where do I submit a bug or enhancement? 

Please use the Github Issue Tracker at [https://github.com/dice-group/LPBenchGen/issues](https://github.com/dice-group/LPBenchGen/issues)
