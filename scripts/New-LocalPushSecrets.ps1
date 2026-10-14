param([string]$Directory = (Join-Path (Split-Path $PSScriptRoot -Parent) '.local/push'))
$ErrorActionPreference = 'Stop'
$destination = [IO.Path]::GetFullPath($Directory)
foreach ($name in @('public.key','private.key','encryption.key')) {
    if (Test-Path -LiteralPath (Join-Path $destination $name)) { throw "Existing push key: $name. Refusing to overwrite." }
}
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$generator = @'
const { generateKeyPairSync, randomBytes } = require('node:crypto');
const { writeFileSync } = require('node:fs');
const { join } = require('node:path');
const directory=process.argv[1];
const {privateKey}=generateKeyPairSync('ec',{namedCurve:'prime256v1'});
const jwk=privateKey.export({format:'jwk'});
const point=Buffer.concat([Buffer.from([4]),Buffer.from(jwk.x,'base64url'),Buffer.from(jwk.y,'base64url')]);
for(const [name,value] of Object.entries({'public.key':point.toString('base64url'),'private.key':jwk.d,'encryption.key':randomBytes(32).toString('base64')}))
  writeFileSync(join(directory,name),value+'\n',{flag:'wx',mode:0o600});
'@
& node -e $generator $destination
if ($LASTEXITCODE -ne 0) { throw 'Push key generation failed.' }
if ($IsWindows) {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls $destination /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restrict the push key directory ACL.' }
}
Write-Output "Push keys created in $destination. Back up encryption.key securely; replacing it makes existing subscriptions unreadable."
