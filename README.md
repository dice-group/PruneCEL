# Details of knowledge graph generation

## Reference knowledge graph details:


We build the knowledge graph QALD9+WD, QALD9+DB, and QALD10. 

The Wikidata reference knowledge graph(WKRF) we utilize for both QALD9+WD and QALD10 are available online: 
- **WKRF** &rarr; (https://zenodo.org/records/7496690). 
  
The DBpedia reference knowledge graph(DBRF) we utilize to build QALD9+DB can be found here: 
- **DBRF** &rarr; (https://downloads.dbpedia.org/2016-10/core-i18n/en/).


## Preprocessing details:
We apply preprocessing on both **WKRF** and **DBRF**. 

For **WKRF**, 
- All occurrences of the property `http://www.wikidata.org/prop/direct/P31` are replaced with `http://www.w3.org/1999/02/22-rdf\textbackslash-syntax-ns\#type`. 

For **DBRF**, 
- $43,618$ triples with IRIs that do not conform to global standards are removed, along with those having properties with the dbp prefix `http://dbpedia.org/property/`. Additionally, for entities belonging to a specific class, the entity type corresponding to all superclasses of that class is also added.

## Knowledge graph question details:

We build the knowledge graph for each dataset. 

  For **QALD10**, the knowledge graph comprises $394$ questions and the associated features. 

  For **QALD9+WK**, the dataset contains $136$ questions, however, only $116$ questions can be answered using the SPARQL queries provided by the gold standard answers, and only the answerable questions are used. 

  For **QALD9+DB**, the dataset contains $150$ questions, only $133$ are utilized for the same reason as above.


## Knowledge graph structure details:

![Ontolearn](Doc/Pic/example_KG.png)
See figure 1, We utilize `Question 1` from the **QALD10** to demonstrate the methodology:

  In **QALD10**, each question is represented by an IRI in the form `dqq:QX`, where `X` denotes the question's serial number.

  Each question has different answer types, represented by properties: `dqb:hasIRIAnswer`, `dqb:hasLiteralAnswer`, and `dqb:hasBooleanAnswer`.

  For questions that have IRI answers, the CBD of the corresponding IRI is extracted from **WKRF**.

  We employ the Stanford NLP toolkit to process each question in English. Key linguistic features are extracted, including the length of the question, the presence of negation, the question word, and the parse tree. Each word in the question is represented using its positional information, for example: `dqq:Q1\_Tree\_T2` denotes the second word in the question, which in this case is `animal`.

  For each question's SPARQL query, we also extract relevant features, including the entities and properties contain within the query itself. The CBD of the entities identified in the SPARQL query is also integrated by using **WKRF**.

# Common question

## Question about PruneCEL

1, Where can I get the knowledge graph?

    For the QALD system, you can get the graph on : URI

    For the Family, Mutagenesis and Carcinogenesis, you can get them on: URI

  
2, How can I run PruneCEL on QALD datasets(QALD10, QALD9+DB, QALD9+WK)?

    Load the knowledge graph using sparql endpoints,

    Run script in script_QALD directory, be care to change the URL(eg. 0.0.0.0:9080), sparql endpoint dir, and PrunCEL Dir to your OWN! 
    
3, Where can I find the learning problems?

    `/T_F_JSON`

## Question about Drill, CELOE

1, Where can I get the embedding and pretrained model for drill?

    Embedding model: 
    Pretrained model: 

2, Does CELOE need embedding?
    

    NO!
