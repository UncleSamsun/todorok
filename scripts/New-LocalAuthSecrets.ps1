param([string]$Directory = (Join-Path $PSScriptRoot '../.local/auth'))
$ErrorActionPreference = 'Stop'
$authDirectory = [System.IO.Path]::GetFullPath($Directory)
New-Item -ItemType Directory -Path $authDirectory -Force | Out-Null
$privatePath = Join-Path $authDirectory 'private.pem'
$publicPath = Join-Path $authDirectory 'public.pem'
if ((Test-Path -LiteralPath $privatePath) -or (Test-Path -LiteralPath $publicPath)) {
    throw 'Key files already exist. Use another directory for intentional key rotation.'
}
$rsa = [System.Security.Cryptography.RSA]::Create(2048)
try {
    $rsa.ExportPkcs8PrivateKeyPem() | Set-Content -LiteralPath $privatePath -Encoding ascii -NoNewline
    $rsa.ExportSubjectPublicKeyInfoPem() | Set-Content -LiteralPath $publicPath -Encoding ascii -NoNewline
} finally { $rsa.Dispose() }
Write-Output "Local authentication keys created in $authDirectory"
