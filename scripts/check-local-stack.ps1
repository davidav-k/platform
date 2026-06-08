$ErrorActionPreference = "Stop"

function Test-ExpectedStatus {
    param(
        [string]$Name,
        [string]$Url,
        [int]$ExpectedStatus
    )

    $actualStatus = 0
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 5
            $actualStatus = [int]$response.StatusCode
        }
        catch {
            if ($null -ne $_.Exception.Response) {
                $actualStatus = [int]$_.Exception.Response.StatusCode
            }
            else {
                $actualStatus = 0
            }
        }

        if ($actualStatus -eq $ExpectedStatus) {
            Write-Host "[OK] $Name returned HTTP $ExpectedStatus."
            return
        }

        Start-Sleep -Seconds 2
    }

    throw "[FAIL] $Name returned HTTP $actualStatus; expected $ExpectedStatus."
}

Test-ExpectedStatus -Name "Notification service health" -Url "http://localhost:8087/actuator/health" -ExpectedStatus 200
Test-ExpectedStatus -Name "Gateway notification route" -Url "http://localhost:8080/api/notifications" -ExpectedStatus 401

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $RepoRoot "compose.yml"
$EnvFile = Join-Path $RepoRoot ".env"

function Fail([string]$Message) {
    Write-Error "ERROR: $Message"
    exit 1
}

function Pass([string]$Message) {
    Write-Host "OK: $Message"
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $EnvFile -f $ComposeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        Fail "Docker Compose command failed."
    }
}

function Test-Http([string]$Name, [string]$Url) {
    try {
        Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 10 -UseBasicParsing | Out-Null
    }
    catch {
        Fail "$Name is not reachable at $Url. Check container logs."
    }

    Pass "$Name is reachable at $Url"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Fail "Docker CLI is not installed or is not on PATH."
}

& docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Fail "Docker Compose v2 is not available. Install a Docker version that supports 'docker compose'."
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Fail "Cannot connect to the Docker daemon. Start Docker Desktop or the Docker service."
}

if (-not (Test-Path $ComposeFile)) {
    Fail "compose.yml was not found at $ComposeFile."
}

if (-not (Test-Path $EnvFile)) {
    Fail ".env was not found. Copy .env.example to .env and set local values."
}

Pass "Docker CLI, Docker Compose, Docker daemon, compose.yml, and .env are available"

$RequiredServices = @(
    "postgres",
    "redis",
    "zipkin",
    "mailhog",
    "config-server",
    "eureka-server",
    "user-service",
    "gateway"
)

$RunningServices = @(Invoke-Compose @("ps", "--services", "--status", "running"))
foreach ($Service in $RequiredServices) {
    if ($RunningServices -notcontains $Service) {
        Fail "Required container for '$Service' is not running. Run 'docker compose --env-file .env -f compose.yml up -d --build'."
    }
}

Pass "All required Docker Compose containers are running"

$HealthyContainers = @(
    "tsp_postgres",
    "tsp_redis",
    "tsp_config",
    "tsp_eureka",
    "tsp_user_service",
    "tsp_gateway"
)

foreach ($Container in $HealthyContainers) {
    $Status = & docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}" $Container 2> $null
    if ($LASTEXITCODE -ne 0) {
        Fail "Cannot inspect '$Container'. Recreate the local stack with Docker Compose."
    }
    if ($Status -ne "healthy") {
        Fail "Container '$Container' health status is '$Status', expected 'healthy'. Check Docker Compose logs."
    }
}

Pass "All containers with Docker health checks are healthy"

& docker exec tsp_postgres pg_isready -q *> $null
if ($LASTEXITCODE -ne 0) {
    Fail "PostgreSQL is not accepting connections inside tsp_postgres."
}
Pass "PostgreSQL is accepting connections"

$RedisResponse = & docker exec tsp_redis redis-cli ping 2> $null
if (($LASTEXITCODE -ne 0) -or ($RedisResponse -ne "PONG")) {
    Fail "Redis did not respond with PONG inside tsp_redis."
}
Pass "Redis responded with PONG"

Test-Http "Config Server" "http://localhost:8888/actuator/health"
Test-Http "Config Server repository" "http://localhost:8888/user-service/dev"
Test-Http "Eureka Server" "http://localhost:8761/actuator/health"
Test-Http "Eureka registry" "http://localhost:8761/eureka/apps"
Test-Http "User Service" "http://localhost:8085/actuator/health"
Test-Http "API Gateway" "http://localhost:8080/actuator/health"

try {
    $RouteResponse = Invoke-WebRequest `
        -Uri "http://localhost:8080/api/users/verify/account?key=local-startup-route-check-not-a-uuid" `
        -Method Get `
        -TimeoutSec 10 `
        -UseBasicParsing
    $RouteStatus = [int]$RouteResponse.StatusCode
}
catch {
    if ($null -eq $_.Exception.Response) {
        Fail "API Gateway could not route the public user-service verification request."
    }
    $RouteStatus = [int]$_.Exception.Response.StatusCode
}

if (($RouteStatus -ne 200) -and ($RouteStatus -ne 400)) {
    Fail "API Gateway user-service route returned HTTP $RouteStatus; expected 200 or 400 from the read-only invalid-key probe."
}

Pass "API Gateway routed a read-only request to User Service (expected invalid-key response: HTTP $RouteStatus)"
Write-Host ""
Write-Host "Local platform stack verification passed."
