:
# QALD10 MST5 Reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
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
    --inputFile ./T_F_Json/QALD10/TandF_MST5_reverse.json \
    --outputFile ./Results/QALD10/MST5_reverse/MST5_reverse_110.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid
COMMENT
# QALD10 MST5 Reverse 111 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10_"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9080/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./T_F_Json/QALD10/TandF_MST5_reverse.json \
    --outputFile ./Results/QALD10/MST5_reverse/MST5_reverse_111.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid




# QALD10 MST5 Reverse 112 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9080/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./T_F_Json/QALD10/TandF_MST5_reverse.json \
    --outputFile ./Results/QALD10/MST5_reverse/MST5_reverse_112.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid



# QALD10 deeppavlov Reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
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
    --inputFile ./T_F_Json/QALD10/TandF_deeppavlov_reverse.json \
    --outputFile ./Results/QALD10/deeppavlov_reverse/deeppavlov_reverse_110.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid

# QALD10 deeppavlov Reverse 111 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9080/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./T_F_Json/QALD10/TandF_deeppavlov_reverse.json \
    --outputFile ./Results/QALD10/deeppavlov_reverse/deeppavlov_reverse_111.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid

# QALD10 deeppavlov Reverse 112 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD10"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo "$(date --iso-8601) Starting script.. !!"
echo "$(date --iso-8601) - Checking Tentris"

set -e  # Stop script on error

cd "$TENTRIS_DIR"

# Check for snapshots and delete older ones if more than 3
snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
while [ ${#snapshots[@]} -gt 3 ]; do
    oldest_snapshot=${snapshots[${#snapshots[@]}-1]}
    ./tentris_new snapshot destroy "$oldest_snapshot"
    snapshots=($(./tentris_new snapshot list | grep auto | awk '{print $1}'))
done

# Start Tentris in the background and store PID
./tentris_new serve 0.0.0.0:9080 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9080 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9080 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"

# Run the KG preprocessor file in Java
cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $PRUNECEL_DIR. Exiting."; exit 1; }
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9080/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./T_F_Json/QALD10/TandF_deeppavlov_reverse.json \
    --outputFile ./Results/QALD10/deeppavlov_reverse/deeppavlov_reverse_112.csv \
    --cluster false \
    --folds 1 \
    --foldTrainTestSavePath Fold/QALD10_MST5

# Stop Tentris
cd "$TENTRIS_DIR"
kill "$(cat tentris.pid)" >/dev/null
if kill -0 "$(cat tentris.pid)" 2>/dev/null; then
    kill -9 "$(cat tentris.pid)" >/dev/null
fi
rm tentris.pid


COMMENT



