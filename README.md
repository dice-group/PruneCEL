# Explainable Benchmarking through the Lense of Concept Learning
This repository contains 
* The source code of our concept learning approach PruneCEL
* Links and descriptions to rerun Experiments I and II
* The survey and its results of Experiment III

## Table of Contents
1. [Repository Structure](README.md#1-repository-structure)
2. [Running Experiments](README.md#2-running-experiments)
3. [Ren Experiment I](README.md#3-run-experiment-i)
4. [Knowledge Base Details](README.md#4-knowledge-base-details)
5. [Run Experiment II](README.md#5-run-experiment-ii)
6. [Details Experiment III](README.md#6-details-experiment-iii)
7. [FAQ](README.md#7-faq)

## 1. Repository Structure
The following directories and files can be found within this project:
```
+-Doc/Pic:                   Pictures used in this README
+-Script_F_C_M:              The scripts that we used to run PruneCEL in Experiment I
+-Script_QALD:               The scripts that we used to run PruneCEL in Experiment II
+-T_F_Json:                  The learning problems of Experiment I and II
+-Experiment_III_Survey.pdf: Details of the survey in Experiment III
+-src:                       The Java source code of PruneCEL
+-pom.xml:                   File necessary to compile PruneCEL with Maven
```

## 2. Running Experiments

### Experiment Setup

PruneCEL uses SPARQL queries to retrieve data from the underlying knowledge base. 
For our experiments, we used the triple store [Tentris](https://github.com/dice-group/Tentris). However, the experiments can be run with any other triple store (e.g., [Fuseki](https://jena.apache.org/documentation/fuseki2/)).
However, using a different triple store can lead to different results since PruneCEL moves a large amount of the work to the triple store, serving as oracle.

For our experiments, we relied on the implementations of the approaches CELOE, Drill, EvoLearner and NCES from the [Ontolearn](https://github.com/dice-group/ontolearn) project. We refer to this project with respect to the execution of these approaches. During our experiments, CELOE and DRILL were set up in a similar way as PruneCEL, i.e., we provided the address of the SPARQL endpoint and both approaches used SPARQL queries to retrieve the necessary data. However, the implementations of EvoLearner and NCES do not seem to support this feature at the moment and both have to load the data into memory before they start. Note that we did not take this loading time into consideration when measuring the runtime of these approaches.

### Compiling PruneCEL

PruneCEL is a Maven project and after downloading this repository, PruneCEL can be compiled using the following command:
```sh
mvn clean package
```
The result of the compilation and packaging process is available as `target/prune-cel-0.0.1-SNAPSHOT.jar`.

## 3. Run Experiment I

Thankfully, the Ontolearn project provides [examples](https://github.com/dice-group/Ontolearn/tree/develop/examples) how to execute the related work approaches (CELOE, Drill, EvoLearner and NCES) on the benchmarking datasets.

To run experiment I on PruneCEL, First, Load the knowledge graph using triple store, to download knowledge graphs:
```shell
wget https://files.dice-research.org/projects/Ontolearn/KGs.zip -O ./KGs.zip && unzip KGs.zip
```

Second, run script in /**Script_F_C_M**, here is an example script:
```shell
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
--sparqlUrl http://localhost:9020/sparql \
--ontology ALC \
--accuracyfunction 0 \
--punishLongExpression true \
--avoidPickySolutionsDecorator true \
--iteration 0 \
--time 60000 \
--recursive true \
--skipNone true \
--inputFile ./././././T_F_Json/Carcinogenesis/lps.json \
--outputFile ./././././Results/Carcinogenesis/Carcinogenesis110.csv \
--cluster false \
--folds 1 \
--foldTrainTestSavePath Fold/Mutagenesis
```

Change sparqlUrl to your own sparql endpoint;

Change accuracyfunction to 0 or 1 or 2, where 0 is F1, 1 is Balance Accuracy, 2 is Accuracy;

Change recursive to 0 or 1, where 0 is PruneCEL and 1 is PruneCEL-R;

Change skipNone to 0 or 1, where 0 is PruneCEL and 1 is PruneCEL-S;

Change intputFile to .Json file in **T_F_Json**;

Change outputFile to the place you want to save your results;

Leave the rest settings along!


## 4. Knowledge Base Details

In this section, we want to give some more details about the knowledge bases that we created for the 3 QA benchmarks ([QALD 9 plus](https://github.com/KGQA/QALD_9_plus) DBpedia, QALD 9 plus Wikidata and [QALD 10](https://github.com/KGQA/QALD-10/)) and the two reference knowledge graphs ([DBpedia](https://downloads.dbpedia.org/2016-10/core-i18n/en/) and [Wikidata](https://zenodo.org/records/7496690)).

### Preprocessing

We remove all questions from the three QA datasets that have an empty ground truth answer set.

We preprocessed the DBpedia reference graph by removing $43,618$ triples with IRIs that do not pass through the RDF checker. We also removed properties of the `http://dbpedia.org/property/` namespace. 
Additionally, we inferred the classes of all entities based on the class hierarchy.


We preprocessed Wikidata by replacing the property `http://www.wikidata.org/prop/direct/P31` with `http://www.w3.org/1999/02/22-rdf\textbackslash-syntax-ns\#type`.


## Knowledge Base Structure

In the first step of our benchmarking framework, we generate a knowledge graph comprising information from the dataset used during the benchmarking process. Our work relies on the QALD datasets, which include three types of data for each question:
1. **Natural language question.** Each question comes with a representation in several languages. From the English question, we extract linguistic features such as \begin{itemize}
   * The length of the question, 
   * The presence of negation, 
   * The question word, and 
   * The NLP parse tree. We employ the Stanford NLP toolkit for the extraction.
3. **Answer(s).** Each question comes with the ground truth answers. We add these answers to the generated graph with three different properties distinguishing IRI, boolean and other literal answers.
    For each IRI listed as answer, we add its concise bounded description (CBD) extracted from the reference knowledge graph.
4. **SPARQL query.** Each question has a SPARQL query that returns the ground truth answer when used on the reference knowledge graph. We adopt LSQ to add the following SPARQL query features to our knowledge graph:
   * Entities and properties contained in the query (including the CBD of the entities),
   * Type of query,
   * The number of triple patterns,
   * The number of basic graph patterns, 
   * The average degree of vertices, 
   * The median degree of vertices involved in join operations,
   * The minimum, maximum, and median number of triple patterns in a basic graph pattern, and
   * The presence of certain keywords such as `FILTER`, `DISTINCT`, and `GROUP BY`.

The following figure shows an example question (Question 1 from QALD10) and the data that we collected for such a question.

![example_KG](Doc/Pic/example_KG.png)

Each question is represented by an IRI in the form `dqq:QX`, where `X` denotes the question's serial number. The questions can have different answer types, represented by different properties: `dqb:hasIRIAnswer`, `dqb:hasLiteralAnswer`, and `dqb:hasBooleanAnswer`. 

## 5. Run Experiment II
### PruneCEL
First, Load the knowledge graph using triple store, to find knowledge graphs:

```shell
https://zenodo.org/records/14720669
```

Second, run script in **Script_QALD**

### Drill
First, Load the knowledge graph using triple store, to find knowledge graphs:

```shell
https://zenodo.org/records/14720669
```

Second, Drill relies on embeddings,to get embedding, please use Dice Embedding model(https://github.com/dice-group/dice-embeddings). we also provide well-trained embedded(https://zenodo.org/records/14720609).

The configuration we use to train embedding:

| Datasets     | Parameter                                                                                                                                                                                           |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **QALD10**   | **Main Parameters**                                                                                                                                                                                 |
|              | `dicee --dataset_dir  KGs/QALD10 --model Keci --embedding_dim 32 --lr 0.1  --save_embeddings_as_csv --num_epochs 1  --batch_size 50000 --optim Adam --scoring_technique NegSample --eval_mode None` |
|              | **Model:** Keci                                                                                                                                                                                     |
|              | **Embedding dimension:** 32                                                                                                                                                                         |
|              | **Learning rate:** 0.1                                                                                                                                                                              |
|              | **Epochs:** 1                                                                                                                                                                                       |
|              | **Batch size:** 50000                                                                                                                                                                               |
|              | **Optimization function:** Adam                                                                                                                                                                     |
|              | **Evaluation mode:** None                                                                                                                                                                           |
|              |                                                                                                                                                                                                     |
| **QALD9+DB** | **Main Parameters**                                                                                                                                                                                 |
|              | `dicee --dataset_dir  KGs/QALD9_WK --model Keci --embedding_dim 8 --lr 0.1  --save_embeddings_as_csv --num_epochs 1  --batch_size 512 --optim Adam --scoring_technique NegSample --eval_mode None`  |
|              | **Model:** Keci                                                                                                                                                                                     |
|              | **Embedding dimension:** 8                                                                                                                                                                          |
|              | **Learning rate:** 0.1                                                                                                                                                                              |
|              | **Epochs:** 1                                                                                                                                                                                       |
|              | **Batch size:** 512                                                                                                                                                                                 |
|              | **Optimization function:** Adam                                                                                                                                                                     |
|              | **Evaluation mode:** None                                                                                                                                                                           |
|              |                                                                                                                                                                                                     |
| **QALD9+WD** | **Main Parameters**                                                                                                                                                                                 |
|              | `dicee --dataset_dir  KGs/QALD9_DB --model Keci --embedding_dim 8 --lr 0.1  --save_embeddings_as_csv --num_epochs 1  --batch_size 512 --optim Adam --scoring_technique NegSample --eval_mode None`  |
|              | **Model:** Keci                                                                                                                                                                                     |
|              | **Embedding dimension:** 8                                                                                                                                                                          |
|              | **Learning rate:** 0.1                                                                                                                                                                              |
|              | **Epochs:** 1                                                                                                                                                                                       |
|              | **Batch size:** 512                                                                                                                                                                                 |
|              | **Optimization function:** Adam                                                                                                                                                                     |
|              | **Evaluation mode:** None                                                                                                                                                                           |







Third, use embedding and knowledge graphs to get pre-trained model, we also provide well-trained pre-trained model for Drill(https://zenodo.org/records/14720524).


In the end, run Drill.


## 6. Details Experiment III

First, we chose two concepts learned on the QALD10 and QALD9+DB. We decided to use the concept learned for the learning problem for which PruneCEL had the largest difference to the baseline approach (which would be the top concept). We verbalized these two concepts using ChatGPT.
After that, we used the verbalizations to conduct a survey as described in our paper.

### Verbalization

#### QALD 10 MST5

The following concept has been learned by PruneCEL:
```
∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(
    http://www.wikidata.org/entity/Q482994
    ⊔
    ∃http://www.wikidata.org/prop/direct/P7763.⊤
    ⊔
    http://www.wikidata.org/entity/Q28640
    ⊔
    ∃http://www.wikidata.org/prop/direct/P131.¬http://www.wikidata.org/entity/Q6256
    ⊔
    ∃http://www.wikidata.org/prop/direct/P1557.⊤
  )
⊔
∃http://w3id.org/dice-research/qa-bench#hasBooleanAnswer.⊤
```
We replaced all IRIs with their English labels and created the following prompt for ChatGPT:
```
A question answering system "QAS1" is able to answer questions that fulfill the following DL expression:
(∃has entity answer.(album⊔∃copyright status as a creator.⊤⊔profession⊔∃located in the administrative territorial entity.¬country⊔∃manifestation of.⊤)⊔∃has boolean answer.⊤)
Explain that in simple words. Keep your answer short.
```
Verbalization:
```
The system "QAS1" can answer questions if:

1. There’s an answer involving an *album*, a *creator's copyright status*, a *profession*, a location that’s *not a country*, or something that has a *type or form*.
2. Or, it can answer questions that have a simple *yes/no (boolean) answer*.
```

#### QALD9+DB MST5

The following concept has been learned by PruneCEL:
```
(
  ∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(
    (
      ¬http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Agent
      ⊓
      ∃http://dbpedia.org/ontology/parentMountainPeak.⊤
    )
    ⊔
    http://www.wikidata.org/entity/Q41176
  )
  ⊔
  (
    ∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(
      http://dbpedia.org/ontology/Astronaut
      ⊔
      (
        ¬http://www.wikidata.org/entity/Q24229398
        ⊓
        ¬http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing
      )
    )
    ⊓
    ∃http://w3id.org/dice-research/qa-bench#hasQuestionWord.⊤
  )
)
```
We replaced all IRIs with their English labels and created the following prompt for ChatGPT:
```
A question answering system "QAS2" is able to answer questions that fulfill the following DL expression:
(∃http://w3id.org/dice-research/qa-bench#has entity answer.((¬agent⊓∃parent mountain peak.⊤)⊔building)⊔(∃has entity answer.(astronaut⊔(¬agent⊓¬spatial thing))⊓∃has question word.⊤))
Explain that in simple words. Keep your answer short.
```
Verbalization:
```
The system "QAS2" can answer questions if:

1. The answer involves either:
   - A *non-agent* (not a person or entity with intent) with a *parent mountain peak*, or
   - A *building*.
2. Or, if the answer involves:
   - An *astronaut*, or
   - A *non-agent, non-spatial entity* (something that’s neither a person nor a physical location),
   - And if the question includes a question word (like "who," "what," or "where").
```

### Survey

The survey and the detailed results can be found in the file `Experiment_III_Survey.pdf` within this repository.



