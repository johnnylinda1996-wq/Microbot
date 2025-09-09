#!/bin/bash
# Quick test script for mule system functionality

echo "=== Mule System Quick Test ==="
echo "Testing bridge server connectivity..."

# Test 1: Health Check
echo "1. Testing Health Check:"
response=$(curl -s -w "%{http_code}" -o /dev/null http://localhost:8080/api/mule/health 2>/dev/null || echo "000")
if [ "$response" = "200" ]; then
    echo "   ✓ Bridge server is healthy"
else
    echo "   ✗ Bridge server not responding (HTTP: $response)"
    echo "   Please start the bridge server first!"
    exit 1
fi

# Test 2: Create a test mule request
echo "2. Creating test mule request:"
request_data='{"location":"Grand Exchange","tradePartner":"TestBot","items":[{"itemId":995,"itemName":"Coins","quantity":100000}]}'
response=$(curl -s -X POST -H "Content-Type: application/json" -d "$request_data" http://localhost:8080/api/mule/request)
echo "   Response: $response"

# Test 3: Check for pending requests
echo "3. Checking for pending requests:"
response=$(curl -s http://localhost:8080/api/mule/next-request)
echo "   Response: $response"

echo ""
echo "=== Test Complete ==="
echo "If you see successful responses above, the mule system is working!"
