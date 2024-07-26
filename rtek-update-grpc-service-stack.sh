#!/bin/bash
# Turning off the AWS pager so that the CLI doesn't open an editor for each command result
export AWS_PAGER=""
# Input log functions
source ./deploy-scripts/shared_func.sh

log_progress "Starting the AWS update deployment steps"

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

IMAGE_URL=$(aws ecr describe-repositories --repository-names $DEFAULT_ECR_REPOSITORY_NAME --query 'repositories[0].repositoryUri' --output text)
# Get the latest version
IMAGE_VERSION=$(aws ecr describe-images --repository-name $DEFAULT_ECR_REPOSITORY_NAME \
    --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]' --output text)

IMAGE_VERSION_WITH_HYPHENS=$(echo "$IMAGE_VERSION" | tr '.' '-')

log_progress "Checking if stack $STACK_NAME-service exists..."
if aws cloudformation describe-stacks --stack-name "$STACK_NAME-service" > /dev/null 2>&1; then
  {
    log_progress "Creating stack changeset for $STACK_NAME-service..."
    aws cloudformation create-change-set \
      --change-set-name update-"$STACK_NAME"-service-"$IMAGE_VERSION_WITH_HYPHENS" \
      --stack-name "$STACK_NAME"-service \
      --use-previous-template \
      --parameters \
          ParameterKey=NetworkStackName,ParameterValue="$STACK_NAME-network" \
          ParameterKey=DatabaseStackName,ParameterValue="$STACK_NAME-database" \
          ParameterKey=ServiceName,ParameterValue="$DEFAULT_ECR_REPOSITORY_NAME" \
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
    log_progress "Waiting for $STACK_NAME-service stack changeset details..."
    aws cloudformation describe-change-set \
      --stack-name "$STACK_NAME"-service  \
      --change-set-name update-"$STACK_NAME"-service-"$IMAGE_VERSION_WITH_HYPHENS"
    log_progress "Retrieved $STACK_NAME-service stack changeset details..."
  }
fi

DEFAULT_PROCEED_PROMPT="n"

get_input "Proceed with changeset?: (y/n)" PROCEED_PROMPT $DEFAULT_PROCEED_PROMPT
if [ "$PROCEED_PROMPT" == "y" ]; then
  log_progress "Executing changeset update-$STACK_NAME-service-$IMAGE_VERSION_WITH_HYPHENS"
  aws cloudformation execute-change-set \
    --stack-name "$STACK_NAME"-service \
    --change-set-name update-"$STACK_NAME"-service-"$IMAGE_VERSION_WITH_HYPHENS"
  aws cloudformation wait stack-update-complete --stack-name "$STACK_NAME"-service
  log_success "Changeset executed successfully"
else
  log_progress "Checking if changeset update-$STACK_NAME-service-$IMAGE_VERSION_WITH_HYPHENS exists"
  CHANGE_SET_DESCRIPTION=$(aws cloudformation describe-change-set \
    --change-set-name update-"$STACK_NAME"-service-"$IMAGE_VERSION_WITH_HYPHENS" \
    --stack-name "$STACK_NAME"-service 2>&1)

  if echo "$CHANGE_SET_DESCRIPTION" | grep -q "ChangeSetNotFound"; then
    log_progress "Changeset does not exist, skipping deletion"
  elif echo "$CHANGE_SET_DESCRIPTION" | grep -q "ValidationError"; then
    log_error "Validation error encountered: $CHANGE_SET_DESCRIPTION"
  elif echo "$CHANGE_SET_DESCRIPTION" | grep -q "AccessDenied"; then
    log_error "Access denied error encountered: $CHANGE_SET_DESCRIPTION"
  else
    log_progress "Deleting changeset update-$STACK_NAME-service-$IMAGE_VERSION_WITH_HYPHENS"
    DELETE_RESPONSE=$(aws cloudformation delete-change-set \
      --change-set-name update-"$STACK_NAME"-service-"$IMAGE_VERSION_WITH_HYPHENS" \
      --stack-name "$STACK_NAME"-service 2>&1)
    if [ $? -eq 0 ]; then
      log_success "Changeset deleted successfully"
    else
      log_error "Error deleting changeset: $DELETE_RESPONSE"
    fi
  fi
fi