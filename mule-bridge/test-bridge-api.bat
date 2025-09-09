@echo off
echo Testing Mule Bridge API Endpoints...
echo.

echo 1. Testing Health Check:
curl -X GET http://localhost:8080/api/mule/health
echo.
echo.

echo 2. Testing Mule Request Creation:
curl -X POST http://localhost:8080/api/mule/request ^
  -H "Content-Type: application/json" ^
  -d "{\"location\":\"Grand Exchange\",\"tradePartner\":\"TestPlayer\",\"items\":[{\"itemId\":995,\"itemName\":\"Coins\",\"quantity\":100000}]}"
echo.
echo.

echo 3. Testing Get Next Request (Mule Client):
curl -X GET http://localhost:8080/api/mule/next-request
echo.
echo.

echo 4. Testing Request Status:
curl -X GET "http://localhost:8080/api/mule/request/1/status"
echo.
echo.

echo Test completed! Check the responses above.
pause
