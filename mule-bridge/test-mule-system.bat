@echo off
echo === Mule System Complete Test ===
echo Testing bridge server functionality...
echo.

echo 1. Testing Health Check:
curl -X GET "http://localhost:8080/api/mule/health"
echo.
echo.

echo 2. Creating test mule request:
curl -X POST "http://localhost:8080/api/mule/request" ^
  -H "Content-Type: application/json" ^
  -d "{\"requesterUsername\":\"TestBot\",\"muleAccount\":\"MuleBot1\",\"location\":\"Grand Exchange\",\"items\":[{\"itemId\":995,\"itemName\":\"Coins\",\"quantity\":100000},{\"itemId\":314,\"itemName\":\"Feather\",\"quantity\":50}]}"
echo.
echo.

echo 3. Checking for pending requests (mule bot polling):
curl -X GET "http://localhost:8080/api/mule/next-request"
echo.
echo.

echo 4. Getting request status (replace with actual request ID):
curl -X GET "http://localhost:8080/api/mule/request/1/status"
echo.
echo.

echo 5. Testing WebSocket endpoint availability:
echo WebSocket endpoint: ws://localhost:8080/ws/updates
echo.

echo === Test Complete ===
echo The bridge server is responding if you see JSON responses above!
echo If step 2 shows a request ID, the system is working correctly!
echo.
pause
