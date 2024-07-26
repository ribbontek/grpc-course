#!/bin/bash

# Input functions
source "$PWD"/deploy-scripts/shared_func.sh

log_progress "Starting the Spring Boot application dockerization and AWS ECR deployment steps"
# Set default values
DEFAULT_ECR_REPOSITORY_NAME="grpc-order-management-service" # also the folder/project name
DEFAULT_ECR_REGION="ap-southeast-2"
DEFAULT_AWS_PROFILE="default"
DEFAULT_INCREMENT_VERSION="minor"

# Retrieve or set variables
get_input "Enter ECR repository name:" ECR_REPOSITORY_NAME $DEFAULT_ECR_REPOSITORY_NAME
get_input "Enter AWS region:" ECR_REGION $DEFAULT_ECR_REGION
get_input "Enter AWS profile:" AWS_PROFILE $DEFAULT_AWS_PROFILE
get_input "Enter Version Increment (patch/minor/major):" INCREMENT_VERSION $DEFAULT_INCREMENT_VERSION

# Build the Spring Boot application
log_progress "Building the Spring Boot application..."
./gradlew clean build -i
if [ $? -eq 0 ]; then
  log_success "Spring Boot application built successfully."
else
  log_error "Failed to build Spring Boot application."
  exit 1
fi

# Version the Spring Boot application
log_progress "Versioning the Spring Boot application..."
case $INCREMENT_VERSION in
  patch)
    VSN_COMMAND="./gradlew $ECR_REPOSITORY_NAME:release -Prelease.disableChecks -Prelease.scope=incrementPatch"
    ;;
  minor)
    VSN_COMMAND="./gradlew $ECR_REPOSITORY_NAME:release -Prelease.disableChecks -Prelease.scope=incrementMinor"
    ;;
  major)
    VSN_COMMAND="./gradlew $ECR_REPOSITORY_NAME:release -Prelease.disableChecks -Prelease.scope=incrementMajor"
    ;;
  *)
    log_error "Unknown increment version type. Use 'patch', 'minor' or 'major'."
    exit 1
    ;;
esac

# Execute Version command
$VSN_COMMAND
if [ $? -eq 0 ]; then
  log_success "Spring Boot application versioned successfully."
else
  log_error "Failed to version Spring Boot application."
  exit 1
fi


IMAGE_TAG=$(git describe --tags --abbrev=0 | cut -d'-' -f1)
log_progress "Build & Tagging Docker image with version ${IMAGE_TAG}"

cd ./grpc-order-management-service/docker && rm -rf certs && mkdir certs \
&& openssl req -x509 -nodes -subj "/CN=localhost" -newkey rsa:4096 -sha256 -keyout certs/server.key -out certs/server.crt -days 3650 \
&& cd ../../
# Build Docker image
log_progress "Building Docker image..."
docker build -t $(basename "$ECR_REPOSITORY_NAME"):latest -t $(basename "$ECR_REPOSITORY_NAME"):"${IMAGE_TAG}" -f ./"$ECR_REPOSITORY_NAME"/docker/Dockerfile .
if [ $? -eq 0 ]; then
  log_success "Docker image built successfully."
else
  log_error "Failed to build Docker image."
  exit 1
fi

# Authenticate Docker to AWS ECR
log_progress "Authenticating Docker to AWS ECR..."
aws ecr get-login-password --region "${ECR_REGION}" --profile "${AWS_PROFILE}" | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr."${ECR_REGION}".amazonaws.com
if [ $? -eq 0 ]; then
  log_success "Docker authenticated to AWS ECR successfully."
else
  log_error "Failed to authenticate Docker to AWS ECR."
  exit 1
fi

# Create ECR repository if it does not exist
log_progress "Creating ECR repository (if it does not exist)..."
aws ecr describe-repositories --repository-names "${ECR_REPOSITORY_NAME}" --region "${ECR_REGION}" --profile "${AWS_PROFILE}" > /dev/null 2>&1
if [ $? -ne 0 ]; then
  aws ecr create-repository --repository-name "${ECR_REPOSITORY_NAME}" --region "${ECR_REGION}" --profile "${AWS_PROFILE}"
  if [ $? -eq 0 ]; then
    log_success "ECR repository created successfully."
  else
    log_error "Failed to create ECR repository."
    exit 1
  fi
else
  log_success "ECR repository already exists."
fi

# Tag Docker image
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
docker tag "${ECR_REPOSITORY_NAME}":latest "${ACCOUNT_ID}".dkr.ecr."${ECR_REGION}".amazonaws.com/"${ECR_REPOSITORY_NAME}":latest
docker tag "${ECR_REPOSITORY_NAME}":"${IMAGE_TAG}" "${ACCOUNT_ID}".dkr.ecr."${ECR_REGION}".amazonaws.com/"${ECR_REPOSITORY_NAME}":"${IMAGE_TAG}"
if [ $? -eq 0 ]; then
  log_success "Docker image tagged successfully."
else
  log_error "Failed to tag Docker image."
  exit 1
fi

# Push Docker image to ECR
log_progress "Pushing Docker image to ECR..."
docker push "${ACCOUNT_ID}".dkr.ecr."${ECR_REGION}".amazonaws.com/"${ECR_REPOSITORY_NAME}":latest
docker push "${ACCOUNT_ID}".dkr.ecr."${ECR_REGION}".amazonaws.com/"${ECR_REPOSITORY_NAME}":"${IMAGE_TAG}"
if [ $? -eq 0 ]; then
  log_success "Docker image pushed to ECR successfully."
else
  log_error "Failed to push Docker image to ECR."
  exit 1
fi

log_success "Spring Boot application Dockerized and published to AWS ECR successfully!"
