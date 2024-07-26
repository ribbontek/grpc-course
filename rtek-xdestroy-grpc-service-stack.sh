#!/bin/bash
# Turning off the AWS pager so that the CLI doesn't open an editor for each command result
export AWS_PAGER=""

# Input log functions
source ./deploy-scripts/shared_func.sh

log_progress "Starting the AWS destroy deployment steps"

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

log_progress "Checking if stack $STACK_NAME-service exists..."
if aws cloudformation describe-stacks --stack-name "$STACK_NAME-service" > /dev/null 2>&1; then
  {
    aws cloudformation delete-stack \
      --stack-name "$STACK_NAME-service"
    aws cloudformation wait stack-delete-complete \
      --stack-name "$STACK_NAME-service"
    log_success "Stack $STACK_NAME-service deletion completed."
  }
fi

log_progress "Checking if stack $STACK_NAME-database exists..."
if aws cloudformation describe-stacks --stack-name "$STACK_NAME-database" > /dev/null 2>&1; then
  {
    aws cloudformation delete-stack \
      --stack-name "$STACK_NAME-database"
    aws cloudformation wait stack-delete-complete \
      --stack-name "$STACK_NAME-database"
    log_success "Stack $STACK_NAME-database deletion completed."
  }
fi

log_progress "Checking if stack $STACK_NAME-network exists..."
if aws cloudformation describe-stacks --stack-name "$STACK_NAME-network" > /dev/null 2>&1; then
  {
    aws cloudformation delete-stack \
      --stack-name "$STACK_NAME-network"
    aws cloudformation wait stack-delete-complete \
      --stack-name "$STACK_NAME-network"
    log_success "Stack $STACK_NAME-network deletion completed."
  }
fi