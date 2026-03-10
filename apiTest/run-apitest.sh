# Simple shell script to run the api test
# We call build-and-build-and-run-docker.sh to do its job
# And then we call docker-compose up and wait for the app to be up
echo "STARTED at $(date)"
./build-and-run-docker.sh

echo "Running docker compose up -d ... and waiting for 8082 /actuator/health"
docker compose up -d
for i in {1..30}; do
    if curl -s http://localhost:8082/actuator/health > /dev/null; then
        echo "App is up"
        break
    fi
    echo "Waiting for app to be up ($i)..."
    sleep 2
done

echo "Running ./gradlew test"
./gradlew test
docker compose down

echo "ENDED at $(date)"





