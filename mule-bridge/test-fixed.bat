@echo off
echo === Fixed Mule System Test ===
echo Testing bridge server with proper JSON formatting...
echo.

echo 1. Testing Health Check:
curl -X GET "http://localhost:8080/api/mule/health"
echo.
echo.

echo 2. Creating test mule request (fixed JSON):
curl -X POST "http://localhost:8080/api/mule/request" ^
  -H "Content-Type: application/json" ^
  -d "{\"requesterUsername\":\"TestBot\",\"muleAccount\":\"MuleBot1\",\"location\":\"Grand Exchange\",\"items\":[{\"itemId\":995,\"itemName\":\"Coins\",\"quantity\":100000}]}"
echo.
echo.

echo 3. Alternative test using file-based JSON:
echo Creating temporary JSON file...
echo {"requesterUsername":"TestBot","muleAccount":"MuleBot1","location":"Grand Exchange","items":[{"itemId":995,"itemName":"Coins","quantity":100000},{"itemId":314,"itemName":"Feather","quantity":50}]} > test_request.json

echo Sending request from file:
curl -X POST "http://localhost:8080/api/mule/request" ^
  -H "Content-Type: application/json" ^
  -d @test_request.json
echo.
echo.

echo 4. Testing mule polling (if request was created):
curl -X GET "http://localhost:8080/api/mule/next-request"
echo.
echo.

echo 5. Testing request status update:
curl -X PUT "http://localhost:8080/api/mule/request/test123/status" ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"IN_PROGRESS\"}"
echo.
echo.

echo === Test Complete ===
echo If you see request IDs in the responses above, the system is working!
del test_request.json 2>nul
echo.
pause
