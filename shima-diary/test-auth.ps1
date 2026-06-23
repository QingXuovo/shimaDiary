# 测试登录和注册接口脚本
Write-Host "=========================================="
Write-Host "    测试登录和注册接口"
Write-Host "=========================================="
Write-Host ""

$baseUrl = "http://localhost:8080/api"
$headers = @{
    "Content-Type" = "application/json"
}

# 测试 1: 注册新用户
Write-Host "测试 1: 注册新用户"
Write-Host "-------------------"
$registerBody = @{
    username = "testuser123"
    password = "test123"
    nickname = "测试用户"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-WebRequest -Uri "$baseUrl/user/register" -Method POST -Headers $headers -Body $registerBody -UseBasicParsing
    $registerResult = $registerResponse.Content | ConvertFrom-Json
    Write-Host "注册响应: $($registerResult | ConvertTo-Json -Depth 5)"
    if ($registerResult.code -eq 200) {
        Write-Host "✅ 注册成功"
    } else {
        Write-Host "⚠️  注册响应: $($registerResult.message)"
    }
} catch {
    Write-Host "❌ 注册失败: $($_.Exception.Message)"
}
Write-Host ""

# 测试 2: 使用 admin 账号登录
Write-Host "测试 2: 使用 admin 账号登录"
Write-Host "-------------------"
$loginBody = @{
    username = "admin"
    password = "123456"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$baseUrl/user/login" -Method POST -Headers $headers -Body $loginBody -UseBasicParsing
    $loginResult = $loginResponse.Content | ConvertFrom-Json
    Write-Host "登录响应: $($loginResult | ConvertTo-Json -Depth 5)"
    if ($loginResult.code -eq 200 -and $loginResult.data) {
        Write-Host "✅ 登录成功"
        Write-Host "用户ID: $($loginResult.data.id)"
        Write-Host "用户名: $($loginResult.data.username)"
        Write-Host "昵称: $($loginResult.data.nickname)"
        Write-Host "角色: $($loginResult.data.role)"
    } else {
        Write-Host "❌ 登录失败: $($loginResult.message)"
    }
} catch {
    Write-Host "❌ 登录失败: $($_.Exception.Message)"
}
Write-Host ""

# 测试 3: 使用错误密码登录
Write-Host "测试 3: 使用错误密码登录"
Write-Host "-------------------"
$wrongLoginBody = @{
    username = "admin"
    password = "wrongpassword"
} | ConvertTo-Json

try {
    $wrongLoginResponse = Invoke-WebRequest -Uri "$baseUrl/user/login" -Method POST -Headers $headers -Body $wrongLoginBody -UseBasicParsing
    $wrongLoginResult = $wrongLoginResponse.Content | ConvertFrom-Json
    Write-Host "登录响应: $($wrongLoginResult | ConvertTo-Json -Depth 5)"
    if ($wrongLoginResult.code -ne 200) {
        Write-Host "✅ 正确拒绝了错误密码"
    } else {
        Write-Host "❌ 错误：接受了错误密码"
    }
} catch {
    Write-Host "❌ 请求失败: $($_.Exception.Message)"
}
Write-Host ""

Write-Host "=========================================="