# SnakeForged Cloud Infrastructure — Cost Estimate

Target: **$10–30/month** (as specified in architecture document).

## Monthly cost breakdown (us-east-1, single dev/staging instance)

| Service | Configuration | Est. $/month |
|---------|---------------|-------------|
| **ECS Fargate** | 0.25 vCPU × 0.5 GB, 730 h/month (always-on) | ~$11 |
| **Application Load Balancer** | 1 ALB, ~1 LCU/h | ~$17 |
| **CloudFront** | PriceClass_100, <1 TB/month (free tier covers first 1 TB) | ~$0 |
| **ACM TLS certificates** | 2 certs (ALB + CF) | $0 |
| **Route 53** | 1 hosted zone + A-record queries | ~$0.50 |
| **CloudWatch Logs** | 30-day retention, ~500 MB/month | ~$0.25 |
| **SSM Parameter Store** | Standard tier | $0 |
| **Data transfer** | ~1 GB/month outbound | ~$0.09 |
| **Total (dev/staging)** | | **~$28–29/month** |

### Production (2 tasks, slightly larger)

| Service | Configuration | Est. $/month |
|---------|---------------|-------------|
| ECS Fargate | 0.5 vCPU × 1 GB × 2 tasks | ~$29 |
| ALB | 1 ALB | ~$17 |
| CloudFront | PriceClass_100 | ~$0 |
| Other | Same as above | ~$1 |
| **Total (prod)** | | **~$47/month** |

> Production with 2 tasks slightly exceeds the $30 target.  Reducing to 1 task
> (desired_count = 1) brings production back to ~$29/month at the cost of brief
> downtime during deployments.

## Cost optimisation options

1. **Use Fargate Spot** for dev/staging — up to 70% discount; acceptable for
   non-production interruptions.
2. **Schedule scale-to-zero for dev** after hours using EventBridge rules.
3. **App Runner** instead of ECS+ALB eliminates the $17 ALB cost; total drops
   to ~$10–12/month but loses some configurability.
4. **CloudFront free tier** covers the first 1 TB of data transfer per month —
   effectively free for this workload level.

## Remote state infrastructure (one-time, not included above)

| Resource | Cost |
|----------|------|
| S3 bucket (state storage, ~100 KB) | < $0.01/month |
| DynamoDB table (state locking, on-demand) | < $0.01/month |
