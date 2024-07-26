#!/bin/bash

# Function to set AWS credentials from the credentials file
set_aws_credentials() {
    local profile=${1:-default}
    local credentials_file="$HOME/.aws/credentials"

    if [ ! -f "$credentials_file" ]; then
        echo "AWS credentials file not found at $credentials_file"
        return 1
    fi

    aws_access_key_id=$(aws --profile "$profile" configure get aws_access_key_id)
    aws_secret_access_key=$(aws --profile "$profile" configure get aws_secret_access_key)
    aws_default_region=$(aws --profile "$profile" configure get aws_region)

    if [ -z "$aws_default_region" ]; then
        aws_default_region="ap-southeast-2"
    fi

    if [ -z "$aws_access_key_id" ] || [ -z "$aws_secret_access_key" ]; then
        echo "Credentials for profile '$profile' are not properly set."
        return 1
    fi

    export AWS_ACCESS_KEY_ID=$aws_access_key_id
    export AWS_SECRET_ACCESS_KEY=$aws_secret_access_key
    export AWS_REGION=$aws_default_region

    echo "AWS credentials for profile '$profile' have been set as environment variables."
}

# Change aws profile here
set_aws_credentials 'default'

# source ./x-setupawscreds.sh - use variables in scope
