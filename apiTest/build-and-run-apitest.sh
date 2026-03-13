# Simple shell script to run the api test
# We call build-and-build-and-run-docker.sh to do its job
# And then we call docker-compose up and wait for the app to be up
set -ex
cd ..
projectname=$(echo $PWD | sed 's/^.*\///')
cd apiTest

echo "STARTED project ${projectname} at $(date)"

./build-docker.sh

echo "Running docker compose up -d ... and waiting for 8082 /actuator/health"
export DOCKER_IMAGE=$projectname

docker compose up -d

echo "Waiting for Service Bus emulator on port 5672 (sqledge + servicebus take ~2 min to initialise)..."
for i in {1..30}; do
  if nc -z localhost 5672 2>/dev/null; then
    echo "Service Bus emulator is accepting connections on port 5672"
    break
  fi
  echo "Waiting for Service Bus ($i/30)..."
  sleep 2
done

echo "Waiting for app on port 8082 /actuator/health..."
for i in {1..30}; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "000")
  if [ "$status" = "200" ]; then
    echo "App is up"
    break
  fi
  echo "Waiting for app to be up ($i/30)... (status=$status)"
  sleep 2
done

echo "Running ./gradlew test"
./gradlew test
docker compose down

echo "ENDED at $(date)"





