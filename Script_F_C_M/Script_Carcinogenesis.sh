
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



