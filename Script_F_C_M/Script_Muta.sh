
# Muta 000 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis000.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"


# Muta 001 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis001.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"


# Muta 002 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis002.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 010 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis010.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 011 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis011.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 012 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone false \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis012.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"


# Muta 100 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis100.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 101 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis101.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 102 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive false \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis102.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 110 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis110.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 111 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis111.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

# Muta 112 ##################################################################################
TARGET_VM="dice-quan.cs.uni-paderborn.de"
TENTRIS_DIR="/data/Tentris_graph/tentris_mutagenesis"
PRUNECEL_DIR="/local/upb/users/q/quannian/profiles/unix/cs/Java_Project/local/upb/users/q/quannian/profiles/unix/cs/Java_Project"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

ssh $TARGET_VM "
    set -e  # Stop script on error
    cd \"$TENTRIS_DIR\"

     # Check for snapshots and delete older ones if more than 3
        snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            while [ \${#snapshots[@]} -gt 3 ]; do
                oldest_snapshot=\${snapshots[\${#snapshots[@]}-1]}
                ./tentris_new snapshot destroy \$oldest_snapshot
                snapshots=(\$(./tentris_new snapshot list | grep auto | awk '{print \$1}'))
            done

    # Start Tentris in the background and store PID
    ./tentris_new serve 0.0.0.0:9030 > tentris_check_output.log 2>&1 &
    pid=\$!
    echo \$pid > tentris.pid
    disown \$pid

    # Sleep for a few seconds to allow logs to populate
    sleep 1

    # Debugging - Check log for specific errors
    if grep -q 'os error 131' tentris_check_output.log; then
        echo 'Error detected: Unable to open datastore. Executing rollback...'
        ./tentris_new rollback --use-latest-auto-snapshot --replay

        # Restart Tentris after rollback
        ./tentris_new serve 0.0.0.0:9030 > tentris_check_output_\$(date +%Y%m%d%H%M%S).log 2>&1 &
        pid=\$!
        echo \$pid > tentris.pid
        disown \$pid
    fi

    # Loop to check if Tentris is available
    max_attempts=60
    attempts=0
    while :
    do
        curl -s 0.0.0.0:9030
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
    --sparqlUrl http://dice-quan.cs.uni-paderborn.de:9030/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 60000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/Mutagenesis/lps.json \
    --outputFile ./././././Results/Mutagenesis/Mutagenesis112.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/Mutagenesis

ssh $TARGET_VM "
    cd \"$TENTRIS_DIR\"
    kill \$(cat tentris.pid) >/dev/null
    if kill -0 \$(cat tentris.pid) 2>/dev/null; then
        kill -9 \$(cat tentris.pid) >/dev/null
    fi
     rm tentris.pid
    exit

"

