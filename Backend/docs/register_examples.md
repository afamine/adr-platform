# Examples: calling POST /api/auth/register

curl example:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName":"Alice Example",
    "email":"alice@example.com",
    "password":"P@ssw0rd1",
    "workspaceName":"My Workspace",
    "workspaceSlug":"my-workspace"
  }'
```

Fetch (browser / frontend) example:

```js
// Ensure you set Content-Type: application/json and send JSON body
async function register() {
  const resp = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: 'Alice Example',
      email: 'alice@example.com',
      password: 'P@ssw0rd1',
      workspaceName: 'My Workspace',
      workspaceSlug: 'my-workspace'
    })
  });
  const data = await resp.json();
  if (!resp.ok) {
    console.error('Registration failed', data);
  } else {
    console.log('Registered', data);
  }
}
```

PowerShell Invoke-RestMethod example:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/register' -ContentType 'application/json' -Body '{"fullName":"Alice Example","email":"alice@example.com","password":"P@ssw0rd1","workspaceName":"My Workspace","workspaceSlug":"my-workspace"}'
```

If you paste the actual request payload your frontend is sending, I can point out exactly which field or header is missing or malformed.

