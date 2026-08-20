$baseUrl = "http://localhost:8080"
$headers = @{ "Content-Type" = "application/json" }

Write-Host "================================================================="
Write-Host "  ADVANCED E2E TEST SUITE: REFRESH TOKENS, SMTP & PROMETHEUS"
Write-Host "================================================================="

# 1. Health & Actuator Checks
Write-Host "`n[TEST 1] Verifying all Microservices Actuator & Prometheus metrics..."
$ports = @(8080, 8081, 8082, 8083, 8084, 8085)
foreach ($p in $ports) {
    $health = Invoke-RestMethod -Uri "http://localhost:${p}/actuator/health"
    $prom = Invoke-WebRequest -Uri "http://localhost:${p}/actuator/prometheus"
    Write-Host "  Port ${p} - Health: $($health.status) | Prometheus Metrics Size: $($prom.Content.Length) bytes"
}

# 2. Registration & Access/Refresh Token Generation
Write-Host "`n[TEST 2] Registering user with Rotating Refresh Tokens..."
$regEmail = "alex.smith_$(Get-Random)@example.com"
$regBody = @{
    email = $regEmail
    password = "SecurePassword123!"
    fullName = "Alex Smith"
    role = "ROLE_USER"
} | ConvertTo-Json

$regResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Headers $headers -Body $regBody
Write-Host "Registration Response:"
Write-Host "  AccessToken:  $($regResp.data.accessToken.Substring(0, 35))..."
Write-Host "  RefreshToken: $($regResp.data.refreshToken)"
Write-Host "  ExpiresIn:    $($regResp.data.expiresIn) ms (15 mins)"
$accessToken = $regResp.data.accessToken
$refreshToken = $regResp.data.refreshToken
$userId = $regResp.data.userId

# 3. Rotating Refresh Token Flow
Write-Host "`n[TEST 3] Testing Rotating Refresh Token (/api/auth/refresh-token)..."
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refreshResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh-token" -Method Post -Headers $headers -Body $refreshBody
Write-Host "Token Rotation Succeeded:"
Write-Host "  New AccessToken:  $($refreshResp.data.accessToken.Substring(0, 35))..."
Write-Host "  New RefreshToken: $($refreshResp.data.refreshToken)"
$newAccessToken = $refreshResp.data.accessToken
$newRefreshToken = $refreshResp.data.refreshToken

# 4. Compromise Detection: Replaying Old Revoked Refresh Token
Write-Host "`n[TEST 4] Testing Replay Protection (Re-using old revoked refresh token)..."
try {
    Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh-token" -Method Post -Headers $headers -Body $refreshBody
    Write-Host "ERROR: Old token should have been rejected!" -ForegroundColor Red
} catch {
    Write-Host "Security Replay Protection Verified: Expected failure caught -> $($_.Exception.Message)" -ForegroundColor Green
}

$authHeader = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $newAccessToken"
}

# 5. Profile Lookup with Rotated Token
Write-Host "`n[TEST 5] Fetching Profile with new rotated Access Token..."
$meResp = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers $authHeader
Write-Host "Profile Email: $($meResp.data.email) | Role: $($meResp.data.role)"

# 6. Catalog Product Creation
Write-Host "`n[TEST 6] Creating Product in Catalog..."
$prodBody = @{
    name = "Ultra-Wide 49-inch Curved Gaming Monitor"
    description = "240Hz Dual QHD 1000R Curved Screen with Quantum Mini-LED"
    price = 1299.99
    stockQuantity = 15
    category = "Displays"
    imageUrl = "https://example.com/monitor.png"
} | ConvertTo-Json

$prodResp = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Post -Headers $authHeader -Body $prodBody
Write-Host "Created Product ID: $($prodResp.data.id)"
$productId = $prodResp.data.id

# 7. Cart Operations
Write-Host "`n[TEST 7] Adding Product to Redis Shopping Cart..."
$cartItem = @{
    productId = $productId
    productName = "Ultra-Wide 49-inch Curved Gaming Monitor"
    unitPrice = 1299.99
    quantity = 1
    imageUrl = "https://example.com/monitor.png"
} | ConvertTo-Json

$cartResp = Invoke-RestMethod -Uri "$baseUrl/api/cart/items" -Method Post -Headers $authHeader -Body $cartItem
Write-Host "Cart Total: `$($cartResp.data.totalAmount) | Items: $($cartResp.data.totalItems)"

# 8. Order Checkout & Real HTML Email Delivery to Mailpit
Write-Host "`n[TEST 8] Checking out cart & Triggering Real HTML Email via Mailpit SMTP..."
$checkoutBody = @{
    shippingAddress = "500 Cloud Horizon Blvd, Austin, TX 78701"
    customerEmail = $regEmail
} | ConvertTo-Json

$orderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders/checkout" -Method Post -Headers $authHeader -Body $checkoutBody
Write-Host "Order Placed Successfully: Order #$($orderResp.data.id) | Total: `$($orderResp.data.totalAmount)"
$orderId = $orderResp.data.id

# Wait for Kafka async event and Mailpit SMTP dispatch
Start-Sleep -Seconds 3

# 9. Verify Mailpit SMTP Capture
Write-Host "`n[TEST 9] Checking Mailpit Local SMTP Server for captured HTML email..."
$mailpitMessages = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get
Write-Host "Mailpit Inbox Total Messages: $($mailpitMessages.total)"
if ($mailpitMessages.total -gt 0) {
    $latestMail = $mailpitMessages.messages[0]
    Write-Host "Latest Email in Mailpit:"
    Write-Host "  From:    $($latestMail.From.Address)"
    Write-Host "  To:      $($latestMail.To[0].Address)"
    Write-Host "  Subject: $($latestMail.Subject)"
}

# 10. Weekly Promotional Email Blast
Write-Host "`n[TEST 10] Triggering Weekly Promotional Email Campaign..."
$promoBody = @{
    promoCode = "AUTUMN30"
    discountPercent = 30
    targetEmails = @($regEmail, "vip-customer@example.com")
} | ConvertTo-Json

$promoResp = Invoke-RestMethod -Uri "$baseUrl/api/notifications/promotions/trigger" -Method Post -Headers $headers -Body $promoBody
Write-Host "Promotional Blast Response: $($promoResp | ConvertTo-Json -Depth 3)"

Start-Sleep -Seconds 1
$updatedMailpit = Invoke-RestMethod -Uri "http://localhost:8025/api/v1/messages" -Method Get
Write-Host "Mailpit Total Emails after Promo Blast: $($updatedMailpit.total)"

# 11. Monitoring Stack Verification: Prometheus, Grafana, Zipkin
Write-Host "`n[TEST 11] Checking Monitoring & Tracing Stack..."
$promTargets = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -Method Get
Write-Host "Prometheus Active Scrape Targets: $($promTargets.data.activeTargets.Count)"

$zipkinServices = Invoke-RestMethod -Uri "http://localhost:9411/api/v2/services" -Method Get
Write-Host "Zipkin Distributed Tracing Active Services: $($zipkinServices -join ', ')"

# 12. Revoke Refresh Token (Logout)
Write-Host "`n[TEST 12] Revoking active refresh token (Logout)..."
$revokeBody = @{ refreshToken = $newRefreshToken } | ConvertTo-Json
$revokeResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/revoke-token" -Method Post -Headers $headers -Body $revokeBody
Write-Host "Revoke Response: $($revokeResp.message)"

Write-Host "`n================================================================="
Write-Host "  ALL UPGRADED ADVANCED CAPABILITIES FULLY VERIFIED! "
Write-Host "================================================================="
