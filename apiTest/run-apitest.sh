# Simple shell script to run the api test
# If DOCKER_IMAGE is set then we use that. Which is what we expect in pipelines
# We currently see app is built on pipelines

# Build sprjng boot fat jarfile in build/libs
# And then build dockerfile
cd ..
projectname=$(echo $PWD | sed 's/^.*\///')
echo "Building projectname $projectname"
./gradlew clean bootjar
docker build -t $projectname .

export DOCKER_IMAGE=$projectname

cd apiTest
docker compose up -d
for i in {1..30}; do
if curl -s http://localhost:8082/actuator/health > /dev/null; then
  echo "App is up"
  break
fi
echo "Waiting for app to be up ($i)..."
sleep 2
done

./gradlew test
docker compose down





