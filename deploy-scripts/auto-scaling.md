

Potential to add in autoscaling at a later date. Another option would be to scale up config in docker file, postgres config, & fargate service config
```yaml
  MyScalingPolicy:
    Type: AWS::ApplicationAutoScaling::ScalingPolicy
    Properties:
      PolicyName: MyScalingPolicy
      PolicyType: TargetTrackingScaling
      ScalingTargetId: 
        Ref: MyScalableTarget
      TargetTrackingScalingPolicyConfiguration:
        TargetValue: 50.0
        PredefinedMetricSpecification:
          PredefinedMetricType: ECSServiceAverageCPUUtilization
        ScaleInCooldown: 60
        ScaleOutCooldown: 60

  MyScalableTarget:
    Type: AWS::ApplicationAutoScaling::ScalableTarget
    Properties:
      MaxCapacity: 5
      MinCapacity: 1
      ResourceId: !Join 
        - ''
        - - service/
          - !Ref MyCluster
          - /
          - !Ref MyService
      RoleARN: arn:aws:iam::123456789012:role/ecsAutoScalingRole
      ScalableDimension: ecs:service:DesiredCount
      ServiceNamespace: ecs

```