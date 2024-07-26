#!/bin/bash

# Variables
USER_POOL_NAME="grpc_course_prod_pool"
APP_CLIENT_NAME="grpc_course_prod_app"
OUTPUT_FILE="cognito_setup_output.txt"

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

# Check if the user pool already exists
echo "Checking if Cognito User Pool '$USER_POOL_NAME' exists..."
USER_POOL_ID=$(user_pool_exists "$USER_POOL_NAME")

if [ -z "$USER_POOL_ID" ]; then
    echo "User Pool '$USER_POOL_NAME' does not exist. Creating a new one..."
    CREATE_USER_POOL_OUTPUT=$(aws cognito-idp create-user-pool --pool-name "$USER_POOL_NAME" --policies 'PasswordPolicy={MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true,RequireSymbols=true}' --auto-verified-attributes email --username-attributes email phone_number)

    if [ $? -ne 0 ]; then
        echo "Error creating user pool" >&2
        exit 1
    fi

    USER_POOL_ID=$(echo $CREATE_USER_POOL_OUTPUT | jq -r '.UserPool.Id')
    echo "User Pool created with ID: $USER_POOL_ID"
else
    echo "User Pool '$USER_POOL_NAME' already exists with ID: $USER_POOL_ID"
    CREATE_USER_POOL_OUTPUT=$(aws cognito-idp describe-user-pool --user-pool-id "$USER_POOL_ID")
fi

# Check if the app client already exists
echo "Checking if Cognito App Client '$APP_CLIENT_NAME' exists in User Pool '$USER_POOL_ID'..."
APP_CLIENT_ID=$(app_client_exists "$USER_POOL_ID" "$APP_CLIENT_NAME")

if [ -z "$APP_CLIENT_ID" ]; then
    echo "App Client '$APP_CLIENT_NAME' does not exist. Creating a new one..."
    CREATE_APP_CLIENT_OUTPUT=$(aws cognito-idp create-user-pool-client --user-pool-id "$USER_POOL_ID" --client-name "$APP_CLIENT_NAME"  --generate-secret --explicit-auth-flows "ALLOW_USER_PASSWORD_AUTH" "ALLOW_REFRESH_TOKEN_AUTH" --enable-token-revocation --prevent-user-existence-errors ENABLED)

    if [ $? -ne 0 ]; then
        echo "Error creating app client" >&2
        exit 1
    fi

    APP_CLIENT_ID=$(echo $CREATE_APP_CLIENT_OUTPUT | jq -r '.UserPoolClient.ClientId')
    echo "App Client created with ID: $APP_CLIENT_ID"
else
    echo "App Client '$APP_CLIENT_NAME' already exists with ID: $APP_CLIENT_ID"
    CREATE_APP_CLIENT_OUTPUT=$(aws cognito-idp describe-user-pool-client --user-pool-id "$USER_POOL_ID" --client-id "$APP_CLIENT_ID")
fi

# Write results to file
echo "Writing results to $OUTPUT_FILE..."
{
    echo "User Pool:"
    echo "$CREATE_USER_POOL_OUTPUT"
    echo ""
    echo "App Client:"
    echo "$CREATE_APP_CLIENT_OUTPUT"
} > "$OUTPUT_FILE"

echo "Setup complete. Results written to $OUTPUT_FILE"
