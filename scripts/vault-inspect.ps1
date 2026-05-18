# Inspect the JWT EC keys stored in Vault.
# The Spring Boot app self-seeds the keys on first boot, so this script is for
# verification / demo purposes (e.g. show the panel during defense).

$ErrorActionPreference = 'Stop'

$VaultAddr  = if ($env:VAULT_URI)      { $env:VAULT_URI }      else { 'http://127.0.0.1:8200' }
$VaultToken = if ($env:VAULT_TOKEN)    { $env:VAULT_TOKEN }    else { 'root' }
$KvPath     = if ($env:VAULT_KV_PATH)  { $env:VAULT_KV_PATH }  else { 'secret/backend/jwt' }

# secret/backend/jwt -> secret + backend/jwt
$parts = $KvPath -split '/', 2
$mount = $parts[0]
$inner = $parts[1]

Write-Host "[vault-inspect] addr=$VaultAddr path=$KvPath"

$headers = @{ 'X-Vault-Token' = $VaultToken }
$url     = "$VaultAddr/v1/$mount/data/$inner"

try {
    $resp = Invoke-RestMethod -Uri $url -Headers $headers -Method GET
    Write-Host ""
    Write-Host "Stored keys at '$KvPath':"
    Write-Host "----------------------------"
    $data = $resp.data.data
    foreach ($k in $data.PSObject.Properties.Name) {
        Write-Host ""
        Write-Host "## $k"
        Write-Host $data.$k
    }
} catch {
    Write-Host "[vault-inspect] No data at '$KvPath' yet — start the Spring Boot app to seed it."
    Write-Host $_.Exception.Message
    exit 1
}
