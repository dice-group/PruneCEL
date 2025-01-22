# No folds
# QALD9 plus dbpedia MST5 reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_MST5_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/MST5_reverse/MST5_reverse_110.csv \
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


# QALD9 plus dbpedia MST5 reverse 111 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_MST5_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/MST5_reverse/MST5_reverse_111.csv \
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


# QALD9 plus dbpedia MST5 reverse 112 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_MST5_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/MST5_reverse/MST5_reverse_112.csv \
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





# QALD9 plus dbpedia Ganswer reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_ganswer_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Ganswer_reverse/Ganswer_reverse_110.csv \
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


# QALD9 plus dbpedia Ganswer reverse 111 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_ganswer_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Ganswer_reverse/Ganswer_reverse_111.csv \
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


# QALD9 plus dbpedia Ganswer reverse 112 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_ganswer_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Ganswer_reverse/Ganswer_reverse_112.csv \
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



# QALD9 plus dbpedia Tebaqa reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_tebaqa_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Tebaqa_reverse/Tebaqa_reverse_110.csv \
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


# QALD9 plus dbpedia Tebaqa reverse 111 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 1 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_tebaqa_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Tebaqa_reverse/Tebaqa_reverse_111.csv \
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


# QALD9 plus dbpedia Tebaqa reverse 112 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 2 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_tebaqa_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/Tebaqa_reverse/Tebaqa_reverse_112.csv \
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






# REPEAT QALD9 plus dbpedia MST5 reverse 110 ##################################################################################

TENTRIS_DIR="/upb/users/q/quannian/profiles/unix/cs/Tentris/QALD9-plus-dbpedia"
PRUNECEL_DIR="/upb/users/q/quannian/profiles/unix/cs/JavaProjects"

echo $(date --iso-8601) " Starting script.. !!"
echo $(date --iso-8601) - Checking Tentris

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
./tentris_new serve 0.0.0.0:9050 > tentris_check_output.log 2>&1 &
pid=$!
echo "$pid" > tentris.pid
disown "$pid"

# Sleep for a few seconds to allow logs to populate
sleep 1

# Debugging - Check log for specific errors
if grep -q 'os error 131' tentris_check_output.log; then
    echo 'Error detected: Unable to open datastore. Executing rollback...'
    ./tentris_new rollback --use-latest-auto-snapshot --replay
    ./tentris_new serve 0.0.0.0:9050 > "tentris_check_output_$(date +%Y%m%d%H%M%S).log" 2>&1 &
    pid=$!
    echo "$pid" > tentris.pid
    disown "$pid"
fi

# Loop to check if Tentris is available
max_attempts=60
attempts=0
while :; do
    curl -s 0.0.0.0:9050 && break
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
        echo 'Tentris did not start within the expected time. Exiting.'
        exit 1
    fi
    sleep 1
done
echo "$(date --iso-8601) - Tentris started and accepting connections"


cd "$PRUNECEL_DIR" || { echo "Failed to change directory to $TENTRIS_DIR. Exiting."; exit 1; }
# First run the KG preprocessor file:
java -cp target/prune-cel-0.0.1-SNAPSHOT.jar org.dice_research.cel.PruneCEL_CLI \
    --sparqlUrl http://localhost:9050/sparql \
    --ontology ALC \
    --accuracyfunction 0 \
    --punishLongExpression true \
    --avoidPickySolutionsDecorator true \
    --iteration 0 \
    --time 600000 \
    --recursive true \
    --skipNone true \
    --inputFile ./././././T_F_Json/QALD9_plus_dbpedia/TandF_MST5_reverse.json \
    --outputFile ./././././Results/QALD9plus-dbpedia/MST5_reverse/MST5_reverse_110.csv \
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




