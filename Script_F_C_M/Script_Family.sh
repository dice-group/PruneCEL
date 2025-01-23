PRUNECEL_DIR="/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"

cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# Run Script

java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9010/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/Family/lps.json \
    --outputFile ./././././Results/Family/Family110.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis



