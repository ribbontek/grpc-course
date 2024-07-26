# Deploys

Deploy the SES Queues stack (us-east-1) 

[//]: # (TODO: Make this more flexible for dev/prod environments)
```
# Create the stack
aws cloudformation create-stack --stack-name GrpcCourseSESStack --template-body file://ses_queues.yaml --region us-east-1
# Check the stack
aws cloudformation describe-stacks --stack-name GrpcCourseSESStack --region us-east-1
# Update the stack
aws cloudformation update-stack --stack-name GrpcCourseSESStack --template-body file://ses_queues.yaml --region us-east-1
```

Deploy the SQS Queues stack - dev/prod (replace stack name GrpcCourseSQSStackProd & param value with prod)
```
# Create the stack
aws cloudformation create-stack --stack-name GrpcCourseSQSStack --template-body file://sqs_queues.yaml --parameters ParameterKey=EnvironmentName,ParameterValue=dev
# Check the stack
aws cloudformation describe-stacks --stack-name GrpcCourseSQSStack
# Update the stack
aws cloudformation update-stack --stack-name GrpcCourseSQSStack --template-body file://sqs_queues.yaml --parameters ParameterKey=EnvironmentName,ParameterValue=dev
```

Deploy the S3 Buckets stack - dev/prod (replace stack name GrpcCourseS3BucketStackProd & param value with prod)
```
# Create the stack
aws cloudformation create-stack --stack-name GrpcCourseS3BucketStack --template-body file://s3_buckets.yaml --parameters ParameterKey=EnvironmentName,ParameterValue=dev
# Check the stack
aws cloudformation describe-stacks --stack-name GrpcCourseS3BucketStack
# Update the stack
aws cloudformation update-stack --stack-name GrpcCourseS3BucketStack --template-body file://s3_buckets.yaml --parameters ParameterKey=EnvironmentName,ParameterValue=dev
```
