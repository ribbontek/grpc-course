#!/bin/bash
# Turning off the AWS pager so that the CLI doesn't open an editor for each command result
export AWS_PAGER=""

# Input log functions
source ./deploy-scripts/shared_func.sh

log_progress "Starting the AWS create deployment steps"

ENVIRONMENT_FILE="$PWD"/rtek-deploy-env-prod.env

# Check if the source file exists
if [ ! -f "$ENVIRONMENT_FILE" ]; then
    log_error "Error: $ENVIRONMENT_FILE not found."
    exit 1
fi

# Source the environment variables
# shellcheck disable=SC1090
source "$ENVIRONMENT_FILE"

log_progress "Loaded environment properties"

DEFAULT_ECR_REPOSITORY_NAME="grpc-order-management-service"

log_progress "Checking if stack $STACK_NAME-network exists..."
if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME-network" > /dev/null 2>&1; then
  {
    log_progress "Searching for certificate with domain $DOMAIN_NAME"
    QUERY="CertificateSummaryList[?DomainName=='${DOMAIN_NAME}'].CertificateArn"
    CERTIFICATE_ARN=$(aws acm list-certificates --query "$QUERY" --output text)
    log_progress "The ARN for the domain $DOMAIN_NAME is: $CERTIFICATE_ARN"

    log_progress "Creating stack $STACK_NAME-network..."
    aws cloudformation create-stack \
      --stack-name "$STACK_NAME-network" \
      --template-body file://network.yml \
      --capabilities CAPABILITY_IAM \
      --parameters \
          ParameterKey=CertificateArn,ParameterValue="$CERTIFICATE_ARN"

    log_progress "Waiting for $STACK_NAME-network stack creation to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$STACK_NAME-network"
    log_success "Stack $STACK_NAME-network creation completed."
  }
else
  {
    log_success "Stack $STACK_NAME-network already exists."
  }
fi

if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME-network" > /dev/null 2>&1; then
  log_error "Failed to create '$STACK_NAME-network'"
  exit 1
fi
# Check available postgresql versions aws rds describe-db-engine-versions --default-only --engine postgres
log_progress "Checking if stack $STACK_NAME-database exists..."
if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME-database" > /dev/null 2>&1; then
  {
    log_progress "Creating stack $STACK_NAME-database..."
    aws cloudformation create-stack \
      --stack-name "$STACK_NAME-database" \
      --template-body file://database.yml \
      --parameters \
          ParameterKey=DBName,ParameterValue=grpccourseprod \
          ParameterKey=NetworkStackName,ParameterValue="$STACK_NAME-network" \
          ParameterKey=DBUsername,ParameterValue=grpccourse

    log_progress "Waiting for $STACK_NAME-database stack creation to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$STACK_NAME-database"
    log_success "Stack $STACK_NAME-database creation completed."
  }
else
  {
    log_success "Stack $STACK_NAME-database already exists."
  }
fi

if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME-database" > /dev/null 2>&1; then
  log_error "Failed to create '$STACK_NAME-database'"
  exit 1
fi

IMAGE_URL=$(aws ecr describe-repositories --repository-names $DEFAULT_ECR_REPOSITORY_NAME --query 'repositories[0].repositoryUri' --output text)
# Get the latest version
IMAGE_VERSION=$(aws ecr describe-images --repository-name $DEFAULT_ECR_REPOSITORY_NAME \
    --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]' --output text)

log_progress "Checking if stack $STACK_NAME-service exists..."
if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME-service" > /dev/null 2>&1; then
  {
    log_progress "Creating stack $STACK_NAME-service..."
    aws cloudformation create-stack \
      --stack-name  "$STACK_NAME-service" \
      --template-body file://service.yml \
      --parameters \
          ParameterKey=NetworkStackName,ParameterValue="$STACK_NAME-network" \
          ParameterKey=DatabaseStackName,ParameterValue="$STACK_NAME-database" \
          ParameterKey=ServiceName,ParameterValue=$DEFAULT_ECR_REPOSITORY_NAME \
          ParameterKey=ImageUrl,ParameterValue="$IMAGE_URL":"$IMAGE_VERSION" \
          ParameterKey=ContainerPort,ParameterValue=9090 \
          ParameterKey=HealthCheckPath,ParameterValue=/grpc.health.v1.Health/Check \
          ParameterKey=HealthCheckIntervalSeconds,ParameterValue=90 \
          ParameterKey=CognitoRegion,ParameterValue="$COGNITO_REGION" \
          ParameterKey=CognitoPoolId,ParameterValue="$COGNITO_POOL_ID" \
          ParameterKey=CognitoClientId,ParameterValue="$COGNITO_CLIENT_ID" \
          ParameterKey=CognitoClientSecret,ParameterValue="$COGNITO_CLIENT_SECRET" \
          ParameterKey=SesRegion,ParameterValue="$SES_REGION" \
          ParameterKey=SesSender,ParameterValue="$SES_SENDER" \
          ParameterKey=SesEmailUrlHeader,ParameterValue="$SES_EMAIL_URL_HEADER" \
          ParameterKey=SesEmailUrlUnsubscribe,ParameterValue="$SES_EMAIL_URL_UNSUBSCRIBE" \
          ParameterKey=SqsQueueComplaintEnabled,ParameterValue="$SQS_QUEUE_COMPLAINT_ENABLED" \
          ParameterKey=SqsQueueBounceEnabled,ParameterValue="$SQS_QUEUE_BOUNCE_ENABLED" \
          ParameterKey=SqsQueueProcessorEnabled,ParameterValue="$SQS_QUEUE_PROCESSOR_ENABLED" \
          ParameterKey=SqsQueueAuditEnabled,ParameterValue="$SQS_QUEUE_AUDIT_ENABLED" \
          ParameterKey=SqsQueueComplaintUri,ParameterValue="$SQS_QUEUE_COMPLAINT_URI" \
          ParameterKey=SqsQueueBounceUri,ParameterValue="$SQS_QUEUE_BOUNCE_URI" \
          ParameterKey=SqsQueueAuditFifoUri,ParameterValue="$SQS_QUEUE_AUDIT_FIFO_URI" \
          ParameterKey=SqsQueueProcessorAsyncUri,ParameterValue="$SQS_QUEUE_PROCESSOR_ASYNC_URI" \
          ParameterKey=SqsQueueEmailRegion,ParameterValue="$SQS_QUEUE_EMAIL_REGION" \
          ParameterKey=SqsQueueGeneralRegion,ParameterValue="$SQS_QUEUE_GENERAL_REGION" \
          ParameterKey=ComRibbontekS3Region,ParameterValue="$COM_RIBBONTEK_S3_REGION" \
          ParameterKey=ComRibbontekS3Bucket,ParameterValue="$COM_RIBBONTEK_S3_BUCKET" \
          ParameterKey=GrpcServerSecurityEnabled,ParameterValue="$GRPC_SERVER_SECURITY_ENABLED" \
          ParameterKey=GrpcServerSecurityCertificateChain,ParameterValue="$GRPC_SERVER_SECURITY_CERTIFICATECHAIN" \
          ParameterKey=GrpcServerSecurityPrivateKey,ParameterValue="$GRPC_SERVER_SECURITY_PRIVATEKEY" \
      --capabilities CAPABILITY_IAM

    log_progress "Waiting for $STACK_NAME-service stack creation to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$STACK_NAME-service"
    log_success "Stack $STACK_NAME-service creation completed."
  } 
else
  {
    log_success "Stack $STACK_NAME-service already exists."
  }
fi



