#!/bin/bash

# Quick API Test Script for Code Reviewers
# Tests all major endpoints with the sample user

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
EMAIL="reviewer@example.com"
PASSWORD="reviewer123"

echo -e "${BLUE}🧪 URL Scanner API Test Suite${NC}"
echo "================================="
echo

# Test 1: Health Check
echo -e "${BLUE}1. Testing Health Check...${NC}"
curl -s "$BASE_URL/actuator/health" | grep -q "UP" && echo -e "${GREEN}✅ Health check passed${NC}" || echo -e "${RED}❌ Health check failed${NC}"
echo

# Test 2: User Login
echo -e "${BLUE}2. Testing User Login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

if [[ $LOGIN_RESPONSE == *"token"* ]]; then
    echo -e "${GREEN}✅ Login successful${NC}"
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo -e "   Token: ${TOKEN:0:20}...${TOKEN: -5}"
else
    echo -e "${RED}❌ Login failed${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi
echo

# Test 3: List Scans
echo -e "${BLUE}3. Testing List Scans...${NC}"
SCANS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/scans")
SCAN_COUNT=$(echo $SCANS_RESPONSE | grep -o '"id"' | wc -l | xargs)
echo -e "${GREEN}✅ Found $SCAN_COUNT scans${NC}"
echo

# Test 4: Create New Scan
echo -e "${BLUE}4. Testing Create Scan...${NC}"
NEW_SCAN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/scans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"url": "https://httpbin.org/get"}')

if [[ $NEW_SCAN_RESPONSE == *"SUBMITTED"* ]]; then
    echo -e "${GREEN}✅ Scan created successfully${NC}"
    SCAN_ID=$(echo $NEW_SCAN_RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo -e "   Scan ID: $SCAN_ID"
else
    echo -e "${RED}❌ Scan creation failed${NC}"
    echo "Response: $NEW_SCAN_RESPONSE"
fi
echo

# Test 5: Get Specific Scan
if [ ! -z "$SCAN_ID" ]; then
    echo -e "${BLUE}5. Testing Get Scan Details...${NC}"
    SCAN_DETAIL=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/scans/$SCAN_ID")
    if [[ $SCAN_DETAIL == *"httpbin.org"* ]]; then
        echo -e "${GREEN}✅ Scan details retrieved${NC}"
    else
        echo -e "${RED}❌ Failed to get scan details${NC}"
    fi
    echo
    
    # Test 6: Delete Scan
    echo -e "${BLUE}6. Testing Delete Scan...${NC}"
    DELETE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
        -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/scans/$SCAN_ID")
    if [ "$DELETE_RESPONSE" = "204" ]; then
        echo -e "${GREEN}✅ Scan deleted successfully${NC}"
    else
        echo -e "${RED}❌ Failed to delete scan (HTTP $DELETE_RESPONSE)${NC}"
    fi
    echo
fi

# Test 7: Unauthorized Access
echo -e "${BLUE}7. Testing Authorization...${NC}"
UNAUTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/scans")
if [ "$UNAUTH_RESPONSE" = "401" ] || [ "$UNAUTH_RESPONSE" = "403" ]; then
    echo -e "${GREEN}✅ Authorization working (HTTP $UNAUTH_RESPONSE)${NC}"
else
    echo -e "${RED}❌ Authorization not working (HTTP $UNAUTH_RESPONSE)${NC}"
fi
echo

# Summary
echo "================================="
echo -e "${GREEN}🎉 API Test Suite Complete!${NC}"
echo
echo -e "${BLUE}Quick Manual Tests:${NC}"
echo "1. Visit: http://localhost:8080/actuator/health"
echo "2. Login with: $EMAIL / $PASSWORD"
echo "3. Use token in Authorization header for API calls"
echo
echo -e "${BLUE}Sample curl commands:${NC}"
echo "# Login and get token:"
echo "curl -X POST $BASE_URL/api/auth/login \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}'"
echo
echo "# List scans (use token from above):"
echo "curl -H 'Authorization: Bearer <TOKEN>' $BASE_URL/api/scans"
echo