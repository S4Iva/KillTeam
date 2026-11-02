# Setzt ein Zugangspasswort der App: schreibt Salt und Hash in AccessGate.kt (das Passwort selbst wird nicht gespeichert).
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\set-access-password.ps1         unbegrenztes Passwort
#         powershell -ExecutionPolicy Bypass -File tools\set-access-password.ps1 -Temp   Passwort, das 7 Tage nach der ersten Eingabe ablaeuft
# Danach die App neu bauen. Leeres Passwort entfernt das jeweilige Passwort (sind beide leer, entfaellt die Abfrage).
# Rundenzahl und Ablauf muessen zu AccessGate.hash passen.
param([switch]$Temp)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$file = Join-Path $root 'shared\src\commonMain\kotlin\com\example\killteam\logic\AccessGate.kt'
$rounds = 20000
$suffix = if ($Temp) { '_TEMP' } else { '' }
$label = if ($Temp) { 'temporaeres Passwort (7 Tage)' } else { 'unbegrenztes Passwort' }

$secure = Read-Host "Neues $label (leer = entfernen)" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))

if ($plain) {
    $confirm = Read-Host 'Passwort wiederholen' -AsSecureString
    $again = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirm))
    if ($plain -ne $again) { throw 'Passwoerter stimmen nicht ueberein.' }
    $bytes = New-Object byte[] 16
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $salt = ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
    $saltBytes = [Text.Encoding]::UTF8.GetBytes($salt)
    $sha = [Security.Cryptography.SHA256]::Create()
    $d = $sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($salt + $plain))
    for ($i = 0; $i -lt $rounds; $i++) { $d = $sha.ComputeHash([byte[]]($d + $saltBytes)) }
    $hash = ($d | ForEach-Object { $_.ToString('x2') }) -join ''
} else { $salt = ''; $hash = '' }

$text = [IO.File]::ReadAllText($file)
$text = [regex]::Replace($text, "const val SALT$suffix = `"[^`"]*`"", "const val SALT$suffix = `"$salt`"")
$text = [regex]::Replace($text, "const val HASH$suffix = `"[^`"]*`"", "const val HASH$suffix = `"$hash`"")
[IO.File]::WriteAllText($file, $text, (New-Object Text.UTF8Encoding($false)))
if ($hash) { Write-Output "$label gesetzt. App neu bauen." } else { Write-Output "$label entfernt. App neu bauen." }
