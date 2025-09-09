@echo off
echo === Complete Mule System Workflow Test ===
echo Testing the full mule trading lifecycle...
echo.

echo 1. Health Check:
curl -X GET "http://localhost:8080/api/mule/health"
echo.
echo.

echo 2. Creating mule request:
echo {"requesterUsername":"TestBot","muleAccount":"MuleBot1","location":"Grand Exchange","items":[{"itemId":995,"itemName":"Coins","quantity":500000},{"itemId":1513,"itemName":"Magic logs","quantity":100}]} > mule_request.json
curl -X POST "http://localhost:8080/api/mule/request" -H "Content-Type: application/json" -d @mule_request.json > request_response.txt
type request_response.txt
echo.
echo.

echo 3. Mule bot polls for requests:
curl -X GET "http://localhost:8080/api/mule/next-request"
echo.
echo.

echo 4. Testing valid status updates:
echo Testing PROCESSING status:
curl -X PUT "http://localhost:8080/api/mule/request/test123/status" -H "Content-Type: application/json" -d "{\"status\":\"PROCESSING\"}"
echo.
echo Testing COMPLETED status:
curl -X PUT "http://localhost:8080/api/mule/request/test123/status" -H "Content-Type: application/json" -d "{\"status\":\"COMPLETED\"}"
echo.
echo Testing FAILED status:
curl -X PUT "http://localhost:8080/api/mule/request/test123/status" -H "Content-Type: application/json" -d "{\"status\":\"FAILED\"}"
echo.
echo.

echo 5. WebSocket Connection Test:
echo WebSocket endpoint available at: ws://localhost:8080/ws/updates
echo This provides real-time status updates for connected clients
echo.

echo === Full System Test Complete ===
echo Valid Status Values: QUEUED, PROCESSING, COMPLETED, FAILED
echo.
del mule_request.json 2>nul
del request_response.txt 2>nul
pause
