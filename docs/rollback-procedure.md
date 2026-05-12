# Rollback Procedure (SnakeForged)

Rolling back returns the ECS service to a **previously deployed container image tag** without changing application code on `main`.

## Prerequisites

- AWS CLI authenticated with permission to describe/register task definitions and update ECS services.
- Known-good image digest or tag previously pushed to ECR (typically a Git SHA short tag from an earlier deployment).
- `ECS_CLUSTER_*` and `ECS_SERVICE_*` names for the target environment (Terraform outputs `ecs_cluster_name`, `ecs_service_name`).

## Roll back by re-deploying a previous image tag

1. **Identify the tag to restore** — from ECR (console or `aws ecr describe-images`), or from the GitHub Actions run that last succeeded for that environment. Image reference format:

   `{ECR_REGISTRY}/{ECR_REPOSITORY}:<tag>`

2. **Deploy that image** (same mechanism as promotions):

   - **Dev**: Re-run workflow "Deploy Dev" on `main`, or invoke `scripts/deploy-ecs.sh` locally/CI:

     ```bash
     export AWS_REGION=us-east-1
     ./scripts/deploy-ecs.sh "$ECS_CLUSTER_DEV" "$ECS_SERVICE_DEV" \
       "${ECR_REGISTRY}/${ECR_REPOSITORY}:${ROLLBACK_TAG}"
     ```

   - **Staging**: In GitHub Actions, run workflow **Promote to Staging** (`promote-staging.yml`) via `workflow_dispatch` and set **`image_tag`** to the rollback tag.

   - **Production**: Run **Promote to Production** (`promote-prod.yml`) with the same **`image_tag`**. Completing production promotion still requires the **Production** GitHub Environment approval gate (minimum one approver).

3. **Wait for ECS steady state**:

   ```bash
   aws ecs wait services-stable \
     --cluster "$ECS_CLUSTER" \
     --services "$ECS_SERVICE" \
     --region "$AWS_REGION"
   ```

4. **Verify** with smoke tests pointing at that environment's public URL:

   ```bash
   ./scripts/smoke-test.sh "$DEPLOY_URL"
   ```

## Notes

- Terraform sets `lifecycle { ignore_changes = [task_definition] }` on the ECS service so image roll-forward/rollback via ECS does not fight routine `terraform apply`.
- Secrets (for example **`OPERATOR_TOKEN`**) remain in **SSM Parameter Store** paths configured per workspace; rollback does not revert SSM values—rotate tokens separately if needed.
- Prefer rolling back to a tag that matched the environment’s **`infrastructure/environments/*.env`** profile (SPRING_PROFILES_ACTIVE, datasource path) expectations to avoid mismatches across long-lived stacks.
