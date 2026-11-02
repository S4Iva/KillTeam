# Erzeugt einen Zugangscode XXXX-XXXX-XXXX fuer die App (siehe anforderungs.md, Abschnitt 4.12).
# Der Code traegt seinen Ablauf und eine Signatur; die App prueft ihn offline mit dem Schluessel aus AccessGate.kt.
#
# Aufruf (genau eine Ablaufangabe):
#   tools\generate-access-code.ps1 -d 7 -h 12            Dauer ab jetzt: 7 Tage und 12 Stunden (beides einzeln oder kombiniert)
#   tools\generate-access-code.ps1 -Until "2026-12-24"   fester Ablauf in UTC; nur Datum = bis Ende dieses Tages (UTC)
#   tools\generate-access-code.ps1 -Until "2026-12-24 18:00"
#   tools\generate-access-code.ps1 -Unlimited            unbegrenzt
#   tools\generate-access-code.ps1 -NewKey               neuen geheimen Schluessel nach AccessGate.kt schreiben (macht alle alten Codes ungueltig, danach App neu bauen)
# Der Ablauf wird auf volle Stunden aufgerundet. -Key und -Nonce sind nur fuer Tests (feste Werte).
param(
    [int]$d = 0,
    [int]$h = 0,
    [string]$Until,
    [switch]$Unlimited,
    [switch]$NewKey,
    [string]$Key,
    [int]$Nonce = -1
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$file = Join-Path $root 'shared\src\commonMain\kotlin\com\example\killteam\logic\AccessGate.kt'
$alphabet = '0123456789ABCDEFGHJKMNPQRSTVWXYZ'
$epoch = [DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [TimeSpan]::Zero)

if ($NewKey) {
    $bytes = New-Object byte[] 16
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $new = ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
    $text = [IO.File]::ReadAllText($file)
    $text = [regex]::Replace($text, 'const val KEY = "[^"]*"', "const val KEY = `"$new`"")
    [IO.File]::WriteAllText($file, $text, (New-Object Text.UTF8Encoding($false)))
    Write-Output 'Neuer Schluessel gesetzt. Alle bisherigen Codes sind ungueltig. App neu bauen.'
    return
}

if (-not $Key) {
    $m = [regex]::Match([IO.File]::ReadAllText($file), 'const val KEY = "([^"]*)"')
    if ($m.Success) { $Key = $m.Groups[1].Value }
}
if (-not $Key) { throw 'Kein Schluessel gesetzt. Zuerst: tools\generate-access-code.ps1 -NewKey' }

$modes = @(($d -gt 0 -or $h -gt 0), [bool]$Until, [bool]$Unlimited) | Where-Object { $_ }
if (@($modes).Count -ne 1) { throw 'Genau eine Angabe noetig: -d/-h, -Until oder -Unlimited.' }

$now = [DateTimeOffset]::UtcNow
$hours = 0
if (-not $Unlimited) {
    if ($Until) {
        $dt = [datetime]::ParseExact($Until, [string[]]@('yyyy-MM-dd HH:mm', 'yyyy-MM-dd'), [Globalization.CultureInfo]::InvariantCulture, 'AssumeUniversal,AdjustToUniversal')
        if ($Until -notmatch ' ') { $dt = $dt.AddDays(1) }
        $end = [DateTimeOffset]::new($dt.Ticks, [TimeSpan]::Zero)
    } else {
        $end = $now.AddDays($d).AddHours($h)
    }
    if ($end -le $now) { throw 'Der Ablauf liegt in der Vergangenheit.' }
    $hours = [int][Math]::Ceiling(($end - $epoch).TotalHours)
    if ($hours -lt 1 -or $hours -gt 1048575) { throw 'Ablauf ausserhalb des gueltigen Bereichs (2026 bis ca. 2145).' }
}
if ($Nonce -lt 0) {
    $r = New-Object byte[] 2
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($r)
    $Nonce = [BitConverter]::ToUInt16($r, 0) -band 0xFFF
}

[uint64]$payload = ([uint64]$hours -shl 12) -bor [uint64]($Nonce -band 0xFFF)
$data = [byte[]]@(
    [byte](($payload -shr 24) -band 255), [byte](($payload -shr 16) -band 255),
    [byte](($payload -shr 8) -band 255), [byte]($payload -band 255)
)
$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Key))
$mac = $hmac.ComputeHash($data)
[uint64]$first = ([uint64]$mac[0] -shl 24) -bor ([uint64]$mac[1] -shl 16) -bor ([uint64]$mac[2] -shl 8) -bor [uint64]$mac[3]
[uint64]$value = ($payload -shl 28) -bor ($first -shr 4)

$chars = foreach ($i in 0..11) { $alphabet[[int](($value -shr (55 - 5 * $i)) -band 31)] }
$code = ((-join $chars) -replace '(.{4})(?=.)', '$1-')

Write-Output "Code: $code"
if ($Unlimited) { Write-Output 'Gueltigkeit: unbegrenzt' }
else { Write-Output ("Gueltig bis: {0:yyyy-MM-dd HH:mm} UTC" -f $epoch.AddHours($hours)) }
