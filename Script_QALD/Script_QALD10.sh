
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9080/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD10/TandF_MST5_reverse.json \
    --outputFile ./././././Results/QALD10/MST5_reverse/MST5_reverse_110.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5
