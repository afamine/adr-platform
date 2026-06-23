# Registration Endpoint Examples

Complete examples for calling the `/api/auth/register` endpoint with correct headers and validation.

## Common Error: Missing Content-Type Header

When submitting a registration form, always include the header:
```
Content-Type: application/json
```

Without this header, Spring will not parse the JSON body, and all fields will be null, causing validation to fail.

---

## 1. cURL (Linux/macOS/Windows)

**Basic example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Johnson",
    "email": "alice@example.com",
    "password": "P@ssw0rd123",
    "workspaceName": "My Workspace",
    "workspaceSlug": "my-workspace"
  }' | jq .
```

**With validation errors (missing workspaceName):**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Bob Smith",
    "email": "bob@example.com",
    "password": "Secret@456"
  }' | jq .
```

**Response on validation error (400):**
```json
{
  "timestamp": "2026-06-11T05:20:00Z",
  "status": 400,
  "message": "Validation failed",
  "path": "/api/auth/register",
  "errors": [
    {
      "field": "workspaceName",
      "message": "must not be blank",
      "code": "NOT_BLANK",
      "rejectedValue": null
    }
  ]
}
```

---

## 2. PowerShell (Windows)

**Basic example:**
```powershell
$body = @{
    fullName = "Carol Davis"
    email = "carol@example.com"
    password = "MyP@ssw0rd456"
    workspaceName = "Team Workspace"
    workspaceSlug = "team-workspace"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/auth/register' `
  -ContentType 'application/json' `
  -Body $body
```

**One-liner:**
```powershell
$data = '{"fullName":"Diana Prince","email":"diana@example.com","password":"Wonder@1234","workspaceName":"Diana Workspace","workspaceSlug":"diana-workspace"}'; Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/register' -ContentType 'application/json' -Body $data
```

**Handling errors in PowerShell:**
```powershell
try {
    $response = Invoke-RestMethod -Method Post `
      -Uri 'http://localhost:8080/api/auth/register' `
      -ContentType 'application/json' `
      -Body $body
    Write-Host "Success: $response"
} catch {
    $errorResponse = $_.ErrorDetails.Message | ConvertFrom-Json
    Write-Host "Error: $($errorResponse.message)"
    
    if ($errorResponse.errors) {
        $errorResponse.errors | ForEach-Object {
            Write-Host "  - $($_.field) [$($_.code)]: $($_.message)"
        }
    }
}
```

---

## 3. JavaScript Fetch API

**Basic example:**
```javascript
const registerData = {
    fullName: "Eve Wilson",
    email: "eve@example.com",
    password: "Eve@Pass2025",
    workspaceName: "Eve's Team",
    workspaceSlug: "eve-team"
};

fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(registerData)
})
.then(response => {
    if (response.ok) {
        return response.json();
    } else if (response.status === 400) {
        return response.json().then(error => {
            throw { type: 'validation', data: error };
        });
    } else {
        throw { type: 'server', status: response.status };
    }
})
.then(data => {
    console.log('Registration successful!', data);
})
.catch(error => {
    if (error.type === 'validation') {
        console.error('Validation failed:');
        error.data.errors.forEach(err => {
            console.error(`  ${err.field} [${err.code}]: ${err.message}`);
        });
    } else {
        console.error('Error:', error);
    }
});
```

**With async/await:**
```javascript
async function registerUser(userData) {
    try {
        const response = await fetch('http://localhost:8080/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const error = await response.json();
            if (response.status === 400) {
                // Validation error with field details
                console.table(error.errors);
            }
            throw new Error(error.message);
        }

        return await response.json();
    } catch (error) {
        console.error('Registration error:', error.message);
        throw error;
    }
}

// Usage:
const newUser = {
    fullName: "Frank Turner",
    email: "frank@example.com",
    password: "Frank@2025Pass",
    workspaceName: "Frank's Space",
    workspaceSlug: "frank-space"
};

registerUser(newUser)
    .then(result => console.log('User created:', result))
    .catch(err => console.error('Failed:', err));
```

---

## 4. Axios (JavaScript/Node.js)

```javascript
const axios = require('axios');

const userData = {
    fullName: "Grace Lee",
    email: "grace@example.com",
    password: "Grace@Pass2026",
    workspaceName: "Grace Team",
    workspaceSlug: "grace-team"
};

axios.post('http://localhost:8080/api/auth/register', userData, {
    headers: { 'Content-Type': 'application/json' }
})
.then(response => {
    console.log('Registration successful:', response.data);
})
.catch(error => {
    if (error.response && error.response.status === 400) {
        const validationErrors = error.response.data.errors;
        console.error('Validation errors:');
        validationErrors.forEach(err => {
            console.error(`  - ${err.field} [${err.code}]: ${err.message}`);
        });
    } else {
        console.error('Error:', error.message);
    }
});
```

---

## 5. Testing with Postman

1. **Create a new POST request** to `http://localhost:8080/api/auth/register`

2. **Set the Headers:**
   ```
   Content-Type: application/json
   ```

3. **Set the Body (raw JSON):**
   ```json
   {
     "fullName": "Henry Martinez",
     "email": "henry@example.com",
     "password": "Henry@Sec2025",
     "workspaceName": "Henry's Workspace",
     "workspaceSlug": "henry-workspace"
   }
   ```

4. **Click Send**

5. **View the response** in the Body tab

---

## Validation Rules Summary

| Field | Required | Min/Max | Pattern/Rules |
|-------|----------|---------|---------------|
| **fullName** | ✓ | - | Text with letters/spaces/apostrophes |
| **email** | ✓ | - | Valid email format (user@domain.com) |
| **password** | ✓ | 8-∞ | Min 1 uppercase, 1 digit |
| **workspaceName** | ✓ | 3-50 | Any text |
| **workspaceSlug** | ✗ | 3-60 | Lowercase letters, digits, hyphens only (e.g., `my-workspace`) |

---

## Error Codes Reference

When validation fails, the response includes error codes for each field:

- **NOT_BLANK**: Field is required but empty
- **INVALID_EMAIL**: Email format is incorrect
- **TOO_SHORT**: Value is shorter than minimum length
- **TOO_LONG**: Value is longer than maximum length
- **INVALID_FORMAT**: Value does not match required pattern

Example error response:
```json
{
  "timestamp": "2026-06-11T05:25:00Z",
  "status": 400,
  "message": "Validation failed",
  "path": "/api/auth/register",
  "errors": [
    {
      "field": "password",
      "message": "Password must contain at least one uppercase letter and one digit",
      "code": "INVALID_FORMAT",
      "rejectedValue": "weakpass123"
    },
    {
      "field": "workspaceSlug",
      "message": "Workspace slug must be lowercase letters, digits and hyphens only",
      "code": "INVALID_FORMAT",
      "rejectedValue": "My-Workspace"
    }
  ]
}
```

---

## Troubleshooting

### Issue: "workspaceName must not be blank"
**Cause:** Content-Type header is missing or set incorrectly.
**Fix:** Add `Content-Type: application/json` header.

### Issue: "Invalid email"
**Cause:** Email format doesn't match pattern `user@domain.com`.
**Fix:** Use a properly formatted email.

### Issue: "Password must contain uppercase and digit"
**Cause:** Password is too weak.
**Fix:** Use a password with at least one uppercase letter and one digit.

### Issue: "409 Conflict - Email already registered"
**Cause:** That email is already used by another account.
**Fix:** Use a different email address.

### Issue: "409 Conflict - Workspace slug already taken"
**Cause:** That workspace slug is already in use.
**Fix:** Choose a different slug.

