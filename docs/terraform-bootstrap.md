# Terraform Bootstrap — One-Time Setup

Before the first `terraform init`, the S3 state bucket and DynamoDB lock table
must exist.  Run these commands once per AWS account:

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION="us-east-1"
BUCKET="snakeforged-tfstate-${ACCOUNT_ID}"
TABLE="snakeforged-tfstate-lock"

# Create S3 bucket with versioning and encryption
aws s3api create-bucket --bucket "$BUCKET" --region "$REGION"
aws s3api put-bucket-versioning \
  --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled
aws s3api put-bucket-encryption \
  --bucket "$BUCKET" \
  --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
aws s3api put-public-access-block \
  --bucket "$BUCKET" \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Create DynamoDB lock table
aws dynamodb create-table \
  --table-name "$TABLE" \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region "$REGION"
```

## Create SSM secrets before applying

```bash
# Prometheus operator token
aws ssm put-parameter \
  --name /snakeforged/operator-token \
  --type SecureString \
  --value "$(openssl rand -hex 32)" \
  --region "$REGION" \
  --overwrite
```

## Initialize and apply

```bash
cd infrastructure/

terraform init \
  -backend-config="bucket=${BUCKET}" \
  -backend-config="key=snakeforged/dev/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="dynamodb_table=${TABLE}"

terraform plan -var-file="environments/dev.tfvars"
terraform apply -var-file="environments/dev.tfvars"
```

## Post-apply smoke test

```bash
APP_URL=$(terraform output -raw application_url)
../scripts/smoke-test.sh "$APP_URL"
```
