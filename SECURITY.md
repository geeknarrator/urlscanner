# Security and Secrets Management

This document explains how to properly manage secrets and sensitive configuration across different environments.

## 🔐 Environment-Based Secret Management

### **Local Development**

1. **Copy the environment template**:
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your local values**:
   ```bash
   # Required: Change these values
   DB_PASSWORD=your-local-db-password
   JWT_SECRET=your-very-long-local-jwt-secret-at-least-64-characters-long
   URLSCAN_API_KEY=your-urlscan-io-api-key
   ```

3. **Never commit `.env` to git** - it's in `.gitignore` for security

### **Docker Compose (Local/Testing)**

1. **Create `.env` file** (as above)
2. **Start services**:
   ```bash
   docker-compose up --build
   ```
3. **Docker Compose automatically reads `.env` file**

### **Production (Kubernetes)**

#### Option 1: Create secrets from files
```bash
# Create production environment file (never commit this!)
cat > .env.production << EOF
DB_PASSWORD=super-secure-production-db-password
JWT_SECRET=extremely-long-and-random-production-jwt-secret-key-for-maximum-security
URLSCAN_API_KEY=your-production-urlscan-api-key
EOF

# Create Kubernetes secrets from file
kubectl create secret generic app-secrets \
  --from-env-file=.env.production \
  --namespace=urlscanner

kubectl create secret generic postgres-secret \
  --from-literal=password=super-secure-production-db-password \
  --namespace=urlscanner

# Clean up the file immediately
rm .env.production
```

#### Option 2: Create secrets manually
```bash
# Database password
kubectl create secret generic postgres-secret \
  --from-literal=password=your-secure-db-password \
  --namespace=urlscanner

# Application secrets
kubectl create secret generic app-secrets \
  --from-literal=jwt-secret=your-64-char-jwt-secret \
  --from-literal=urlscan-api-key=your-api-key \
  --namespace=urlscanner
```

#### Option 3: Use external secret management
```bash
# AWS Secrets Manager example
kubectl create secret generic app-secrets \
  --from-literal=jwt-secret="$(aws secretsmanager get-secret-value --secret-id prod/urlscanner/jwt-secret --query SecretString --output text)" \
  --namespace=urlscanner
```

## 🔒 Secret Generation Best Practices

### **Database Password**
```bash
# Generate secure random password
openssl rand -base64 32
```

### **JWT Secret Key**
```bash
# Generate 64+ character random string for HS256
openssl rand -base64 64
```

### **Base64 Encoding for Kubernetes**
```bash
# Encode secrets for Kubernetes YAML
echo -n "your-secret-value" | base64

# Decode to verify
echo "encoded-value" | base64 -d
```

## ⚠️ Security Warnings

### **DO NOT**:
- ❌ Commit `.env` files to git
- ❌ Use default secrets in production
- ❌ Share secrets in plain text (Slack, email, etc.)
- ❌ Use short JWT secrets (< 64 characters)
- ❌ Hardcode secrets in source code

### **DO**:
- ✅ Use strong, random secrets in production
- ✅ Rotate secrets regularly
- ✅ Use different secrets per environment
- ✅ Limit secret access with RBAC
- ✅ Monitor secret access

## 🔧 Environment-Specific Configurations

### **Local Development** (`.env`)
```bash
JWT_SECRET=local-dev-secret-can-be-shorter-for-convenience
JWT_EXPIRATION=86400000  # 24 hours - longer for dev convenience
```

### **Production** (Kubernetes Secrets)
```bash
JWT_SECRET=extremely-long-random-production-key-minimum-64-characters-for-hs256-security
JWT_EXPIRATION=3600000   # 1 hour - shorter for security
```

## 🔍 Verification Commands

### **Check Docker Compose secrets**:
```bash
docker-compose exec app env | grep -E "(DB_PASSWORD|JWT_SECRET)"
```

### **Check Kubernetes secrets**:
```bash
kubectl get secrets -n urlscanner
kubectl describe secret app-secrets -n urlscanner
```

### **Verify secret values** (be careful with output):
```bash
kubectl get secret app-secrets -n urlscanner -o jsonpath='{.data.jwt-secret}' | base64 -d
```

## 🚀 Cloud Platform Examples

### **AWS EKS with Secrets Manager**
```yaml
# Use AWS Load Balancer Controller secrets
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
```

### **Azure AKS with Key Vault**
```yaml
# Use Azure Key Vault CSI driver
apiVersion: v1
kind: SecretProviderClass
metadata:
  name: urlscanner-secrets
spec:
  provider: azure
  parameters:
    keyvaultName: urlscanner-keyvault
```

### **Google GKE with Secret Manager**
```yaml
# Use Google Secret Manager CSI driver
apiVersion: v1
kind: SecretProviderClass
metadata:
  name: urlscanner-secrets
spec:
  provider: gcp
  parameters:
    secrets: |
      - resourceName: "projects/PROJECT_ID/secrets/jwt-secret/versions/latest"
        path: "jwt-secret"
```

---

**Remember**: Security is only as strong as your weakest secret. Always use strong, unique secrets in production!