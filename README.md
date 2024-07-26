# grpc course

A gRPC Course Demo application using SpringBoot 3.2, Kotlin 1.9, Java 21 & Postgres

## Set Up 

### Java 21

Installing Java 21 on Linux/Ubuntu

- `sudo apt-get install openjdk-21-jdk`

### Properties & Dev Environment

In order to run the integration tests successfully, create an `application-integration.properties` file under the resources directory for grpc-order-management-service as it needs the AWS Cognito & SQS properties configured to run

Additionally, under the deploy folder, it is recommended to set up your development resources using the cloud-formation scripts provided, with a verified email already configured in AWS SES. The email no-reply@ribbontek.com is already enabled & verified in AWS SES for testing purposes

## Build the Project

Building the whole project:

```shell
./gradlew clean build -i
```

Building the order-management project:

```shell
./gradlew grpc-order-management-service:clean grpc-order-management-service:build -i
```

Example building the grpc-stubs project for any grpc protobuf file changes

```shell
/gradlew grpc-stubs:build -i
```

## Format the Project with KtLint

```shell
./gradlew ktlintFormat -i
```

## Performing a release

### Publish Service to AWS ECR

This script takes care of building, tagging, setting up a new ECR repository & pushing docker images 
```shell
./rtek-publish-grpc-service.sh
```

### Deploy to AWS

Kudos to reflectoring for the basis of cloudformation deployment configurations: https://reflectoring.io/aws-cloudformation-rds/

Deployment requires pre-requisites S3, SES, SQS & Cognito dependencies ready for all environments

Deploy the stack
```shell
./rtek-deploy-grpc-service-stack.sh
```

Update the stack after publishing another version:
```shell
./rtek-update-grpc-service-stack.sh
```

Destroy the stack (starts with an x to prevent accidental conflict with deploy)
```shell
./rtek-xdestroy-grpc-service-stack.sh
```





