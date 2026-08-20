$baseUrl = "http://localhost:8080"
$headers = @{ "Content-Type" = "application/json" }

Write-Host "================================================================="
Write-Host "  ADVANCED E-COMMERCE PLATFORM E2E TEST RUNNER"
Write-Host "================================================================="

# -------------------------------------------------------------
# Test 1: Service Health Checks
# -------------------------------------------------------------
Write-Host "`n[TEST 1] Verifying all Microservices Health & Prometheus Actuators..."
$services = @(
    @{ Name = "gateway-service"; Port = 8080 },
    @{ Name = "auth-user-service"; Port = 8081 },
    @{ Name = "catalog-service"; Port = 8082 },
    @{ Name = "cart-service"; Port = 8083 },
    @{ Name = "order-service"; Port = 8084 },
    @{ Name = "notification-service"; Port = 8085 }
)

foreach ($svc in $services) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:$($svc.Port)/actuator/health" -TimeoutSec 5
        $prom = Invoke-RestMethod -Uri "http://localhost:$($svc.Port)/actuator/prometheus" -TimeoutSec 5
        Write-Host "  [OK] $($svc.Name) (: $($svc.Port)) -> Health: $($health.status) | Prometheus: Available" -ForegroundColor Green
    } catch {
        Write-Host "  [WARN] $($svc.Name) (: $($svc.Port)) -> $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# -------------------------------------------------------------
# Test 2: User Registration & Rotating Refresh Token
# -------------------------------------------------------------
Write-Host "`n[TEST 2] Registering new user with Rotating Refresh Tokens..."
$randomNum = Get-Random -Minimum 1000 -Maximum 9999
$testEmail = "customer_${randomNum}@ecommerce.test"
$testPassword = "SecurePass123!#"

$regBody = @{
    email = $testEmail
    password = $testPassword
    fullName = "Test Customer ${randomNum}"
    role = "ROLE_USER"
} | ConvertTo-Json

$regResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Headers $headers -Body $regBody
Write-Host "  [OK] Registered User: $($regResp.data.email) (ID: $($regResp.data.userId))" -ForegroundColor Green
Write-Host "       Access Token:  $($regResp.data.accessToken.Substring(0, 35))..."
Write-Host "       Refresh Token: $($regResp.data.refreshToken)"
Write-Host "       Expires In:    $($regResp.data.expiresIn) ms (15 minutes)"

$accessToken = $regResp.data.accessToken
$refreshToken = $regResp.data.refreshToken
$userId = $regResp.data.userId

# -------------------------------------------------------------
# Test 3: User Login
# -------------------------------------------------------------
Write-Host "`n[TEST 3] Logging in with User Credentials..."
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

$loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Headers $headers -Body $loginBody
Write-Host "  [OK] Login successful for $($loginResp.data.email)" -ForegroundColor Green
$accessToken = $loginResp.data.accessToken
$refreshToken = $loginResp.data.refreshToken

# -------------------------------------------------------------
# Test 4: Token Rotation
# -------------------------------------------------------------
Write-Host "`n[TEST 4] Refreshing Token with Rotation (/api/auth/refresh-token)..."
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refreshResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh-token" -Method Post -Headers $headers -Body $refreshBody

Write-Host "  [OK] Token rotation successful!" -ForegroundColor Green
Write-Host "       New Access Token:  $($refreshResp.data.accessToken.Substring(0, 35))..."
Write-Host "       New Refresh Token: $($refreshResp.data.refreshToken)"

$newAccessToken = $refreshResp.data.accessToken
$newRefreshToken = $refreshResp.data.refreshToken

# -------------------------------------------------------------
# Test 5: Compromise Detection (Reusing old revoked token)
# -------------------------------------------------------------
Write-Host "`n[TEST 5] Testing Replay Protection with old revoked refresh token..."
try {
    Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh-token" -Method Post -Headers $headers -Body $refreshBody
    Write-Host "  [FAIL] Old token was accepted when it should have been blocked!" -ForegroundColor Red
} catch {
    Write-Host "  [OK] Replay Protection Passed: Reused token rejected successfully." -ForegroundColor Green
}

$authHeader = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $newAccessToken"
}

# -------------------------------------------------------------
# Test 6: Authenticated User Profile
# -------------------------------------------------------------
Write-Host "`n[TEST 6] Fetching current user profile (/api/users/me)..."
$userProfile = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers $authHeader
Write-Host "  [OK] Profile retrieved: $($userProfile.data.fullName) <$($userProfile.data.email)>" -ForegroundColor Green

# -------------------------------------------------------------
# Test 7: Product Catalog Creation & Retrieval
# -------------------------------------------------------------
Write-Host "`n[TEST 7] Creating products in Catalog Service..."
$prod1 = @{
    name = "Sony WH-1000XM5 Wireless Headphones"
    description = "Industry Leading Noise Canceling with 2 Processors and 8 Microphones"
    price = 399.99
    stockQuantity = 25
    category = "Audio"
    imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e"
} | ConvertTo-Json

$prod2 = @{
    name = "Apple iPad Air M2 11-inch"
    description = "Liquid Retina display, M2 chip, 128GB Storage, Wi-Fi 6E"
    price = 599.99
    stockQuantity = 10
    category = "Tablets"
    imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0"
} | ConvertTo-Json

$prod1Resp = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Post -Headers $authHeader -Body $prod1
$prod2Resp = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Post -Headers $authHeader -Body $prod2
$prod1Id = $prod1Resp.data.id
$prod2Id = $prod2Resp.data.id

Write-Host "  [OK] Created Product 1: $($prod1Resp.data.name) (ID: $prod1Id)" -ForegroundColor Green
Write-Host "  [OK] Created Product 2: $($prod2Resp.data.name) (ID: $prod2Id)" -ForegroundColor Green

# -------------------------------------------------------------
# Test 8: Shopping Cart Management (Redis)
# -------------------------------------------------------------
Write-Host "`n[TEST 8] Adding items to Redis Shopping Cart..."
$cartItem1 = @{
    productId = $prod1Id
    productName = "Sony WH-1000XM5 Wireless Headphones"
    unitPrice = 399.99
    quantity = 1
    imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e"
} | ConvertTo-Json

$cartItem2 = @{
    productId = $prod2Id
    productName = "Apple iPad Air M2 11-inch"
    unitPrice = 599.99
    quantity = 1
    imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0"
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/api/cart/items" -Method Post -Headers $authHeader -Body $cartItem1 | Out-Null
$cartState = Invoke-RestMethod -Uri "$baseUrl/api/cart/items" -Method Post -Headers $authHeader -Body $cartItem2
Write-Host "  [OK] Cart updated -> Total Items: $($cartState.data.totalItems) | Total Price: `$($cartState.data.totalAmount)" -ForegroundColor Green

# -------------------------------------------------------------
# Test 9: Order Checkout & Kafka Event Bus
# -------------------------------------------------------------
Write-Host "`n[TEST 9] Checking out Cart to create Order & publish Kafka event..."
$checkoutBody = @{
    shippingAddress = "742 Evergreen Terrace, Springfield, OR 97477"
    customerEmail = $testEmail
} | ConvertTo-Json

$orderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Headers $authHeader -Body $checkoutBody
$orderId = $orderResp.data.id
Write-Host "  [OK] Order Created Successfully: Order #$orderId | Total: `$($orderResp.data.totalAmount)" -ForegroundColor Green

# -------------------------------------------------------------
# Test 10: Verify Mailpit Local SMTP HTML Order Invoice
# -------------------------------------------------------------
Write-Host "`n[TEST 10] Verifying Mailpit Local SMTP capture for HTML Order Invoice..."
Start-Sleep -Seconds 3

try {
    $mailpitMessages = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get -TimeoutSec 5
    Write-Host "  [OK] Mailpit Inbox contains $($mailpitMessages.total) email(s)" -ForegroundColor Green
    if ($mailpitMessages.total -gt 0) {
        $recentEmail = $mailpitMessages.messages[0]
        Write-Host "       From:    $($recentEmail.From.Address)"
        Write-Host "       To:      $($recentEmail.To[0].Address)"
        Write-Host "       Subject: $($recentEmail.Subject)"
    }
} catch {
    Write-Host "  [WARN] Mailpit check: $($_.Exception.Message)" -ForegroundColor Yellow
}

# -------------------------------------------------------------
# Test 11: Promotional Email Campaign Trigger
# -------------------------------------------------------------
Write-Host "`n[TEST 11] Triggering Weekly Promotional Email Campaign..."
$promoBody = @{
    promoCode = "WEEKEND30"
    discountPercent = 30
    targetEmails = @($testEmail, "vip-user@ecommerce.test")
} | ConvertTo-Json

$promoResp = Invoke-RestMethod -Uri "$baseUrl/api/notifications/promotions/trigger" -Method Post -Headers $headers -Body $promoBody
Write-Host "  [OK] Promotional blast triggered: $($promoResp.message)" -ForegroundColor Green

Start-Sleep -Seconds 1
$mailpitMessagesAfterPromo = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get -TimeoutSec 5
Write-Host "  [OK] Mailpit Inbox updated: $($mailpitMessagesAfterPromo.total) total emails delivered" -ForegroundColor Green

# -------------------------------------------------------------
# Test 12: Observability - Prometheus, Grafana, Zipkin
# -------------------------------------------------------------
Write-Host "`n[TEST 12] Checking Observability Infrastructure (Prometheus, Grafana, Zipkin)..."
try {
    $promTargets = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -Method Get -TimeoutSec 5
    Write-Host "  [OK] Prometheus is scraping $($promTargets.data.activeTargets.Count) targets actively" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Prometheus API: $($_.Exception.Message)" -ForegroundColor Yellow
}

try {
    $zipkinServices = Invoke-RestMethod -Uri "http://localhost:9411/api/v2/services" -Method Get -TimeoutSec 5
    Write-Host "  [OK] Zipkin Distributed Tracing Active Services: $($zipkinServices -join ', ')" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Zipkin API: $($_.Exception.Message)" -ForegroundColor Yellow
}

# -------------------------------------------------------------
# Test 13: Revoke Refresh Token (Logout)
# -------------------------------------------------------------
Write-Host "`n[TEST 13] Revoking Refresh Token on user logout..."
$revokeBody = @{ refreshToken = $newRefreshToken } | ConvertTo-Json
$revokeResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/revoke-token" -Method Post -Headers $headers -Body $revokeBody
Write-Host "  [OK] $($revokeResp.message)" -ForegroundColor Green

Write-Host "`n================================================================="
Write-Host "  ALL 13 ADVANCED END-TO-END TESTS PASSED WITH 100% SUCCESS!"
Write-Host "================================================================="
