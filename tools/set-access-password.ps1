# Setzt das Zugangspasswort der App: schreibt Salt und Hash in AccessConfig.kt (das Passwort selbst wird nicht gespeichert).
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\set-access-password.ps1   (danach App neu bauen)
# Leeres Passwort entfernt die Abfrage. Rundenzahl und Ablauf muessen zu AccessGate.hash passen.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$file = Join-Path $root 'shared\src\commonMain\kotlin\com\example\killteam\logic\AccessGate.kt'
$rounds = 20000

$secure = Read-Host 'Neues Passwort (leer = Abfrage deaktivieren)' -AsSecureString
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
$text = [regex]::Replace($text, 'const val SALT = "[^"]*"', "const val SALT = `"$salt`"")
$text = [regex]::Replace($text, 'const val HASH = "[^"]*"', "const val HASH = `"$hash`"")
[IO.File]::WriteAllText($file, $text, (New-Object Text.UTF8Encoding($false)))
if ($hash) { Write-Output 'Passwort gesetzt. App neu bauen.' } else { Write-Output 'Abfrage deaktiviert. App neu bauen.' }

