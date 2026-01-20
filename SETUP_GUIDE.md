# Setup Guide - Cache Consistency Checker

This guide will help you set up and deploy the complete CI/CD pipeline for your DevOps project.

## Table of Contents
1. [GitHub Repository Setup](#1-github-repository-setup)
2. [DockerHub Configuration](#2-dockerhub-configuration)
3. [GitHub Secrets Configuration](#3-github-secrets-configuration)
4. [Pushing Code to GitHub](#4-pushing-code-to-github)
5. [Testing Locally](#5-testing-locally)
6. [Verifying CI/CD Pipeline](#6-verifying-cicd-pipeline)
7. [Kubernetes Deployment](#7-kubernetes-deployment)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. GitHub Repository Setup

### Create a New Repository

1. Go to [GitHub](https://github.com)
2. Click on "New Repository"
3. Fill in the details:
   - **Repository name**: `cache-consistency-checker`
   - **Description**: "Cache Invalidation Consistency Checker - DevOps CI/CD Project"
   - **Visibility**: Public (or Private)
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)
4. Click "Create repository"

### Note Your Repository URL

After creating, note your repository URL:
```
https://github.com/<YOUR_USERNAME>/cache-consistency-checker.git
```

---

## 2. DockerHub Configuration

### Create DockerHub Account (if you don't have one)

1. Go to [DockerHub](https://hub.docker.com)
2. Click "Sign Up"
3. Fill in your details
4. Verify your email

### Create Access Token

1. Log in to DockerHub
2. Click on your username (top right)
3. Select "Account Settings"
4. Click on "Security"
5. Click "New Access Token"
6. Fill in:
   - **Description**: `GitHub Actions CI/CD`
   - **Access permissions**: `Read, Write, Delete`
7. Click "Generate"
8. **IMPORTANT**: Copy the token immediately (you won't see it again!)
   - Token looks like: `dckr_pat_xxxxxxxxxxxxxxxxxxxxxx`

---

## 3. GitHub Secrets Configuration

### Add Secrets to Your Repository

1. Go to your GitHub repository
2. Click on "Settings" tab
3. In the left sidebar, click "Secrets and variables" → "Actions"
4. Click "New repository secret"

### Add DOCKERHUB_USERNAME

- **Name**: `DOCKERHUB_USERNAME`
- **Value**: Your DockerHub username (e.g., `johndoe`)
- Click "Add secret"

### Add DOCKERHUB_TOKEN

- **Name**: `DOCKERHUB_TOKEN`
- **Value**: The access token you copied earlier (e.g., `dckr_pat_xxxxxx...`)
- Click "Add secret"

### Verify Secrets

Your secrets page should show:
```
DOCKERHUB_USERNAME    *** (Updated X minutes ago)
DOCKERHUB_TOKEN       *** (Updated X minutes ago)
```

---

## 4. Pushing Code to GitHub

### Initialize Git Repository

Open terminal and navigate to the project directory:

```bash
cd ~/cache-consistency-checker

# Initialize git repository
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: Cache Consistency Checker with CI/CD pipeline

- Implemented cache invalidation consistency checker
- Added complete CI/CD pipeline with 11 stages
- Integrated security scanning (SAST, SCA, Container Scan)
- Added Kubernetes deployment manifests
- Comprehensive documentation and tests

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

### Connect to GitHub

```bash
# Add your GitHub repository as remote
git remote add origin https://github.com/<YOUR_USERNAME>/cache-consistency-checker.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

### Verify Push

1. Go to your GitHub repository
2. You should see all files uploaded
3. Check that the `.github/workflows/` directory contains `ci.yml` and `cd.yml`

---

## 5. Testing Locally

### Before Pushing, Test Everything Works

#### Run Tests

```bash
cd ~/cache-consistency-checker

# Run unit tests
mvn test

# Expected output: All tests pass
```

#### Build Application

```bash
# Build the JAR
mvn clean package

# Expected output: BUILD SUCCESS
```

#### Test Docker Build

```bash
# Build Docker image
docker build -t cache-consistency-checker:test .

# Run container
docker run -d -p 8080:8080 --name test-app cache-consistency-checker:test

# Wait 30 seconds for startup
sleep 30

# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected output: {"status":"UP"}

# Test API
curl http://localhost:8080/api/db/all

# Expected output: []

# Stop and remove container
docker stop test-app && docker rm test-app
```

---

## 6. Verifying CI/CD Pipeline

### Monitor CI Pipeline Execution

1. Go to your GitHub repository
2. Click on "Actions" tab
3. You should see "CI Pipeline - Cache Consistency Checker" running
4. Click on the workflow run to see details

### Expected Pipeline Flow

```
✓ Setup & Checkout
✓ Code Quality - Checkstyle
✓ SAST - CodeQL Analysis
✓ SAST - SpotBugs Analysis
✓ SCA - OWASP Dependency Check
✓ Unit Tests
✓ Build Application
✓ Build Docker Image
✓ Container Security - Trivy Scan
✓ Container Runtime Validation
✓ Push Docker Image to DockerHub (only on main branch)
✓ Pipeline Summary
```

### Check for Failures

If any stage fails:

1. Click on the failed job
2. Expand the failed step
3. Read the error message
4. Fix the issue locally
5. Commit and push again

### Common Issues

**CodeQL fails**: This is normal on first run. CodeQL needs to build the project first.

**Dependency Check takes too long**: First run downloads CVE database (can take 10-15 minutes).

**Docker push fails**: Check your DOCKERHUB_USERNAME and DOCKERHUB_TOKEN secrets.

---

## 7. Kubernetes Deployment

### Option 1: Local Kubernetes (Minikube)

#### Install Minikube

```bash
# macOS
brew install minikube

# Start Minikube
minikube start

# Verify
kubectl get nodes
```

#### Deploy Application

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Apply all manifests
kubectl apply -f k8s/

# Check deployment
kubectl get pods -n cache-consistency
kubectl get services -n cache-consistency

# Access application
kubectl port-forward service/cache-consistency-checker 8080:80 -n cache-consistency

# Test
curl http://localhost:8080/actuator/health
```

### Option 2: Cloud Kubernetes (GKE, EKS, AKS)

#### For Google Kubernetes Engine (GKE)

```bash
# Configure kubectl
gcloud container clusters get-credentials <CLUSTER_NAME> --zone <ZONE>

# Deploy
kubectl apply -f k8s/

# Get external IP
kubectl get service cache-consistency-checker -n cache-consistency
```

#### Update Deployment Image

```bash
# After CI pipeline pushes to DockerHub
kubectl set image deployment/cache-consistency-checker \
  cache-consistency-checker=<YOUR_DOCKERHUB_USERNAME>/cache-consistency-checker:latest \
  -n cache-consistency

# Watch rollout
kubectl rollout status deployment/cache-consistency-checker -n cache-consistency
```

---

## 8. Troubleshooting

### Issue: "GitHub Actions workflow not triggering"

**Solution**:
- Ensure `.github/workflows/ci.yml` is in the correct location
- Check that you pushed to `main` or `master` branch
- Verify workflow file has no YAML syntax errors

### Issue: "Docker push unauthorized"

**Solution**:
- Verify DOCKERHUB_USERNAME is correct (check for typos)
- Ensure DOCKERHUB_TOKEN is a valid access token
- Check token has Read/Write permissions

### Issue: "Tests failing in CI but pass locally"

**Solution**:
- Check Java version (should be 17)
- Ensure all dependencies are in `pom.xml`
- Check for hardcoded paths or environment-specific config

### Issue: "Container fails health check"

**Solution**:
- Increase `initialDelaySeconds` in deployment.yaml
- Check container logs: `docker logs <container_id>`
- Verify application starts successfully

### Issue: "Kubernetes pods in CrashLoopBackOff"

**Solution**:
```bash
# Check pod logs
kubectl logs <pod-name> -n cache-consistency

# Describe pod
kubectl describe pod <pod-name> -n cache-consistency

# Common fixes:
# - Increase memory limits
# - Fix image pull secrets
# - Check ConfigMap values
```

### Issue: "OWASP Dependency Check fails"

**Solution**:
- This is common due to CVEs in dependencies
- Check the report: `target/dependency-check-report.html`
- Update vulnerable dependencies in `pom.xml`
- Or suppress false positives

### Issue: "Trivy finds vulnerabilities"

**Solution**:
- Update base Docker image to latest
- Update application dependencies
- For false positives, create `.trivyignore` file

---

## Next Steps

### After Successful Deployment

1. **Test the Application**
   ```bash
   # Create a record
   curl -X POST http://localhost:8080/api/db/create \
     -H "Content-Type: application/json" \
     -d '{"value": "test-data", "cacheImmediately": true}'

   # Check drift
   curl http://localhost:8080/api/analyze/drift
   ```

2. **Monitor the CI/CD Pipeline**
   - Make a small change
   - Commit and push
   - Watch the pipeline run

3. **Prepare Your Demo**
   - Show the GitHub Actions workflow
   - Explain each CI/CD stage
   - Demonstrate the application
   - Show drift detection in action

4. **Prepare for Viva**
   - Understand why each pipeline stage exists
   - Be ready to explain security scanning results
   - Know how to troubleshoot failures
   - Understand the drift detection algorithm

---

## Submission Checklist

Before submitting your project:

- [ ] GitHub repository is public/accessible
- [ ] README.md is complete
- [ ] CI/CD pipeline runs successfully
- [ ] All tests pass
- [ ] Docker image pushed to DockerHub
- [ ] Kubernetes manifests tested
- [ ] PROJECT_REPORT.md completed
- [ ] No hardcoded secrets in code
- [ ] GitHub repository URL added to report

---

## Project Submission

### Convert Report to PDF

```bash
# Install pandoc (if not installed)
brew install pandoc

# Convert markdown to PDF
cd ~/cache-consistency-checker
pandoc PROJECT_REPORT.md -o YourName_StudentID_DevOps_CI_Report.pdf \
  --pdf-engine=pdflatex \
  -V geometry:margin=1in

# Or use online converters:
# - https://www.markdowntopdf.com/
# - https://cloudconvert.com/md-to-pdf
```

### Submit via Google Form

1. Fill in the submission form with:
   - Your name and Student ID
   - GitHub repository URL
   - DockerHub image URL
   - Project report (PDF)

2. Double-check all information

3. Submit before deadline (20th Jan, 2026)

---

## Support

If you encounter issues not covered here:

1. Check GitHub Actions logs
2. Review Docker container logs
3. Check Kubernetes pod logs
4. Consult the README.md
5. Review the assignment requirements

---

## Good Luck!

You now have a production-grade DevOps CI/CD pipeline. This demonstrates:
- Comprehensive security integration
- Automated testing and quality checks
- Container best practices
- Kubernetes deployment
- Industry-standard DevOps practices

Remember: **It's not just about running the pipeline, but understanding WHY each stage exists and what risks it mitigates.**

---

**Created with Claude Code**
**Date: January 2026**
