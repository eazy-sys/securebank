import requests
import json

url = "http://localhost:8080/api/auth/register"
data = {
    "name": "Test User",
    "email": "testuser@example.com",
    "password": "password123",
    "phoneNumber": "1234567890",
    "address": "123 Test St",
    "countryCode": "US"
}
headers = {"Content-Type": "application/json"}

try:
    response = requests.post(url, data=json.dumps(data), headers=headers)
    print("Status Code:", response.status_code)
    print("Response:", response.text)
except Exception as e:
    print("Error:", e) 