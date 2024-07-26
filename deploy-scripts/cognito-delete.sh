#!/bin/bash

# Variables
USER_POOL_NAME="grpc_course_prod_pool"
APP_CLIENT_NAME="grpc_course_prod_app"
OUTPUT_FILE="cognito_delete_output.txt"

# Function to check if a user pool exists
function user_pool_exists {
    local pool_name=$1
    aws cognito-idp list-user-pools --max-results 60 | jq -r --arg pool_name "$pool_name" '.UserPools[] | select(.Name == $pool_name) | .Id'
}

# Function to check if an app client exists
function app_client_exists {
    local user_pool_id=$1
    local client_name=$2
    aws cognito-idp list-user-pool-clients --user-pool-id "$user_pool_id" --max-results 60 | jq -r --arg client_name "$client_name" '.UserPoolClients[] | select(.ClientName == $client_name) | .ClientId'
}

# Check if the user pool exists
echo "Checking if Cognito User Pool '$USER_POOL_NAME' exists..."
USER_POOL_ID=$(user_pool_exists "$USER_POOL_NAME")

if [ -n "$USER_POOL_ID" ]; then
    echo "User Pool '$USER_POOL_NAME' exists with ID: $USER_POOL_ID"

    # Check if the app client exists
    echo "Checking if Cognito App Client '$APP_CLIENT_NAME' exists in User Pool '$USER_POOL_ID'..."
    APP_CLIENT_ID=$(app_client_exists "$USER_POOL_ID" "$APP_CLIENT_NAME")

    if [ -n "$APP_CLIENT_ID" ]; then
        echo "App Client '$APP_CLIENT_NAME' exists with ID: $APP_CLIENT_ID. Deleting..."
        DELETE_APP_CLIENT_OUTPUT=$(aws cognito-idp delete-user-pool-client --user-pool-id "$USER_POOL_ID" --client-id "$APP_CLIENT_ID")

        if [ $? -ne 0 ]; then
            echo "Error deleting app client" >&2
            exit 1
        fi

        echo "App Client '$APP_CLIENT_NAME' deleted."
    else
        echo "App Client '$APP_CLIENT_NAME' does not exist."
    fi

    echo "Deleting User Pool '$USER_POOL_NAME'..."
    DELETE_USER_POOL_OUTPUT=$(aws cognito-idp delete-user-pool --user-pool-id "$USER_POOL_ID")

    if [ $? -ne 0 ]; then
        echo "Error deleting user pool" >&2
        exit 1
    fi

    echo "User Pool '$USER_POOL_NAME' deleted."
else
    echo "User Pool '$USER_POOL_NAME' does not exist."
fi

# Write results to file
echo "Writing results to $OUTPUT_FILE..."
{
    echo "User Pool Deletion Output:"
    echo "$DELETE_USER_POOL_OUTPUT"
    echo ""
    echo "App Client Deletion Output:"
    echo "$DELETE_APP_CLIENT_OUTPUT"
} > "$OUTPUT_FILE"

echo "Deletion complete. Results written to $OUTPUT_FILE"
