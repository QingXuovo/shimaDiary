# 测试登录接口脚本
Write-Host "=========================================="
Write-Host "    测试登录接口"
Write-Host "=========================================="
Write-Host ""

# 定义测试参数
$baseUrl = "http://localhost:8080/api"
$loginEndpoint = "$baseUrl/user/login"
$testUsername = "admin"
$testPassword = "123456"

Write-Host "测试地址: $loginEndpoint"
Write-Host "测试用户: $testUsername"
Write-Host ""

try {
    # 构造请求体
    $body = @{
        username = $testUsername
        password = $testPassword
    } | ConvertTo-Json

    # 设置请求头
    $headers = @{
        "Content-Type" = "application/json"
    }

    Write-Host "正在发送登录请求..."
    Write-Host "请求体: $body"
    Write-Host ""

    # 发送登录请求
    $response = Invoke-WebRequest -Uri $loginEndpoint -Method POST -Headers $headers -Body $body -UseBasicParsing

    # 解析响应
    $result = $response.Content | ConvertFrom-Json

    Write-Host "响应状态码: $($response.StatusCode)"
    Write-Host "响应内容:"
    Write-Host ($result | ConvertTo-Json -Depth 10)
    Write-Host ""

    # 验证响应
    if ($result.code -eq 200 -and $result.data) {
        Write-Host "✅ 测试通过！登录成功"
        Write-Host "用户ID: $($result.data.id)"
        Write-Host "用户名: $($result.data.username)"
        Write-Host "昵称: $($result.data.nickname)"
        Write-Host "角色: $($result.data.role)"
    } else {
        Write-Host "❌ 测试失败！登录失败"
        Write-Host "错误信息: $($result.message)"
    }
} catch {
    Write-Host "❌ 请求失败！"
    Write-Host "错误类型: $($_.Exception.GetType().Name)"
    Write-Host "错误信息: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        Write-Host "响应状态码: $($_.Exception.Response.StatusCode.value__)"
    }
}

Write-Host ""
Write-Host "=========================================="