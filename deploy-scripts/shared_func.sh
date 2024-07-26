#!/bin/bash

# Shared Functions

# Function to log the progress
log_progress() {
  local message=$1
  echo -e "\033[1;34m$message\033[0m"
}

# Function to log success
log_success() {
  local message=$1
  echo -e "\033[1;32m$message\033[0m"
}

# Function to log error
log_error() {
  local message=$1
  echo -e "\033[1;31m$message\033[0m"
}

# Function to retrieve input if not passed as arguments, with default value
get_input() {
  local prompt=$1
  local input_variable=$2
  local default_value=$3

  if [ -z "${!input_variable}" ]; then
    read -p "$prompt [$default_value]: " input
    export $input_variable="${input:-$default_value}"
  fi
}