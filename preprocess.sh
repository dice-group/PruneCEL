TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_family"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error

    cd \"$TENTRIS_DIR\"

    # Start Tentris in the background and store PID
    ./tentris serve 0.0.0.0:9010 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris serve 0.0.0.0:9010 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9010
        if [ \$? -eq 0 ]; then
            break
        fi
        attempts=\$((attempts + 1))
        if [ \$attempts -ge \$max_attempts ]; then
            echo 'Tentris did not start within the expected time. Exiting.'
            exit 1
        fi
        sleep 1
    done

    echo \$(date --iso-8601) - Tentris started and accepting connections
    exit
"






cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9010/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././Fold/Family/Training/AuntTrain_Fold_2.json \
    --outputFile ./././././Fold/Family/Result/Test.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Family



ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\" || { echo '$(date --iso-8601) - Failed to change directory to $TENTRIS_DIR. Exiting.'; exit 1; }

    kill \$(cat tentris.pid) >/dev/null


    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
    exit

"




# rm tentris.pid

