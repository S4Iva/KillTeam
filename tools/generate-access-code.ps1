# Erzeugt einen Zugangscode fuer die App (siehe anforderungs.ModP, Abschnitt 4.12).
# Der Code traegt seinen Ablauf und eine Ed25519-Signatur (109 Zeichen, in 5er-Gruppen). Die App prueft sie offline mit dem
# oeffentlichen Schluessel aus AccessGate.kt; den privaten Schluessel (tools\access-private-key.txt, nicht einchecken!) hat nur dieses Skript.
#
# Aufruf (genau eine Ablaufangabe):
#   tools\generate-access-code.ps1 -d 7 -h 12            Dauer ab jetzt: 7 Tage und 12 Stunden (beides einzeln oder kombiniert)
#   tools\generate-access-code.ps1 -Until "2026-12-24"   fester Ablauf in UTC; nur Datum = bis Ende dieses Tages (UTC)
#   tools\generate-access-code.ps1 -Until "2026-12-24 18:00"
#   tools\generate-access-code.ps1 -Unlimited            unbegrenzt
#   tools\generate-access-code.ps1 -NewKey               neues Schluesselpaar erzeugen, oeffentlichen Schluessel nach AccessGate.kt schreiben
#                                                        (macht alle alten Codes ungueltig, danach App neu bauen; mit -Force ein vorhandenes Paar ersetzen)
# Der Ablauf wird auf volle Stunden aufgerundet. -Key (privater Schluessel als 64 Hex-Zeichen) und -Nonce sind nur fuer Tests.
param(
    [int]$d = 0,
    [int]$h = 0,
    [string]$Until,
    [switch]$Unlimited,
    [switch]$NewKey,
    [switch]$Force,
    [string]$Key,
    [int]$Nonce = -1
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Numerics
$root = Split-Path -Parent $PSScriptRoot
$file = Join-Path $root 'shared\src\commonMain\kotlin\com\example\killteam\logic\AccessGate.kt'
$keyFile = Join-Path $PSScriptRoot 'access-private-key.txt'
$alphabet = '0123456789ABCDEFGHJKMNPQRSTVWXYZ'
$epoch = [DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [TimeSpan]::Zero)

# --- Ed25519 (RFC 8032) mit BigInteger; langsam, aber fuer einzelne Codes ausreichend ---
$BI = [System.Numerics.BigInteger]
$Zero = $BI::Zero
$One = $BI::One
$Prime = $BI::Pow($BI::new(2), 255) - $BI::new(19)
$Order = $BI::Pow($BI::new(2), 252) + $BI::Parse('27742317777372353535851937790883648493')

function ModP($a) { $r = $BI::Remainder($a, $Prime); if ($r.Sign -lt 0) { $r + $Prime } else { $r } }
function Inv($a) { $BI::ModPow($a, $Prime - $BI::new(2), $Prime) }
function FromLE([byte[]]$b) { $BI::new([byte[]]($b + [byte]0)) }
function ToLE32($n) {
    $b = $n.ToByteArray()
    $o = New-Object byte[] 32
    [Array]::Copy($b, $o, [Math]::Min($b.Length, 32))
    , $o
}

$CurveD = ModP ($BI::new(-121665) * (Inv ($BI::new(121666))))
$By = ModP ($BI::new(4) * (Inv ($BI::new(5))))
$x2 = ModP (($By * $By - $One) * (Inv (ModP ($CurveD * $By * $By + $One))))
$Bx = $BI::ModPow($x2, ($Prime + $BI::new(3)) / $BI::new(8), $Prime)
if (-not (ModP ($Bx * $Bx - $x2)).IsZero) { $Bx = ModP ($Bx * $BI::ModPow($BI::new(2), ($Prime - $One) / $BI::new(4), $Prime)) }
if (-not $Bx.IsEven) { $Bx = $Prime - $Bx }
$Base = @($Bx, $By)

function Add-Point($pa, $pb) {
    $t = ModP ($CurveD * $pa[0] * $pb[0] * $pa[1] * $pb[1])
    $x3 = ModP (($pa[0] * $pb[1] + $pb[0] * $pa[1]) * (Inv (ModP ($One + $t))))
    $y3 = ModP (($pa[1] * $pb[1] + $pa[0] * $pb[0]) * (Inv (ModP ($One - $t))))
    @($x3, $y3)
}
function Mul-Point($pt, $k) {
    $bytes = ToLE32 $k
    $r = @($Zero, $One)
    for ($i = 255; $i -ge 0; $i--) {
        $r = Add-Point $r $r
        if ((($bytes[[int][Math]::Floor($i / 8)] -shr ($i % 8)) -band 1) -eq 1) { $r = Add-Point $r $pt }
    }
    $r
}
function Encode-Point($pt) {
    $e = [byte[]](ToLE32 $pt[1])
    if (-not $pt[0].IsEven) { $e[31] = $e[31] -bor 0x80 }
    , $e
}
function Get-Secret([byte[]]$seed) {
    $hs = [Security.Cryptography.SHA512]::Create().ComputeHash($seed)
    $a = [byte[]]($hs[0..31])
    $a[0] = $a[0] -band 248
    $a[31] = ($a[31] -band 127) -bor 64
    @{ Scalar = (FromLE $a); Prefix = [byte[]]($hs[32..63]) }
}
function Sign-Message([byte[]]$seed, [byte[]]$msg) {
    $sha = [Security.Cryptography.SHA512]::Create()
    $s = Get-Secret $seed
    $Aenc = [byte[]](Encode-Point (Mul-Point $Base $s.Scalar))
    $r = (FromLE ($sha.ComputeHash([byte[]]($s.Prefix + $msg)))) % $Order
    $Renc = [byte[]](Encode-Point (Mul-Point $Base $r))
    $k = (FromLE ($sha.ComputeHash([byte[]]($Renc + $Aenc + $msg)))) % $Order
    $sig = ($r + $k * $s.Scalar) % $Order
    , [byte[]]($Renc + (ToLE32 $sig))
}
function To-Hex([byte[]]$b) { ($b | ForEach-Object { $_.ToString('x2') }) -join '' }
function From-Hex([string]$s) { [byte[]](0..($s.Length / 2 - 1) | ForEach-Object { [Convert]::ToByte($s.Substring(2 * $_, 2), 16) }) }

if ($NewKey) {
    if ((Test-Path $keyFile) -and -not $Force) { throw "Es gibt schon einen privaten Schluessel ($keyFile). Ein Wechsel macht alle alten Codes ungueltig; mit -Force trotzdem ersetzen." }
    $seed = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($seed)
    $pub = To-Hex ([byte[]](Encode-Point (Mul-Point $Base (Get-Secret $seed).Scalar)))
    [IO.File]::WriteAllText($keyFile, (To-Hex $seed) + "`n")
    $text = [IO.File]::ReadAllText($file)
    $text = [regex]::Replace($text, 'const val PUBLIC_KEY = "[^"]*"', "const val PUBLIC_KEY = `"$pub`"")
    [IO.File]::WriteAllText($file, $text, (New-Object Text.UTF8Encoding($false)))
    Write-Output "Neues Schluesselpaar. Privater Schluessel: $keyFile (sichern, nicht einchecken). Alle bisherigen Codes sind ungueltig. App neu bauen."
    return
}

$custom = [bool]$Key
if (-not $Key) {
    if (-not (Test-Path $keyFile)) { throw 'Kein privater Schluessel. Zuerst: tools\generate-access-code.ps1 -NewKey' }
    $Key = ([IO.File]::ReadAllText($keyFile)).Trim()
}
if ($Key -notmatch '^[0-9a-fA-F]{64}$') { throw 'Der private Schluessel muss aus 64 Hex-Zeichen bestehen.' }
$seed = From-Hex $Key
if (-not $custom) {
    $m = [regex]::Match([IO.File]::ReadAllText($file), 'const val PUBLIC_KEY = "([^"]*)"')
    $pub = To-Hex ([byte[]](Encode-Point (Mul-Point $Base (Get-Secret $seed).Scalar)))
    if (-not $m.Success -or $m.Groups[1].Value -ne $pub) { throw 'Der private Schluessel passt nicht zum oeffentlichen Schluessel in AccessGate.kt.' }
}

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

[uint32]$payload = ([uint32]$hours -shl 12) -bor [uint32]($Nonce -band 0xFFF)
$data = [byte[]]@(
    [byte](($payload -shr 24) -band 255), [byte](($payload -shr 16) -band 255),
    [byte](($payload -shr 8) -band 255), [byte]($payload -band 255)
)
$bytes = [byte[]]($data + (Sign-Message $seed $data))

$sb = New-Object Text.StringBuilder
$buffer = 0
$bits = 0
foreach ($b in $bytes) {
    $buffer = ($buffer -shl 8) -bor $b
    $bits += 8
    while ($bits -ge 5) {
        $bits -= 5
        [void]$sb.Append($alphabet[($buffer -shr $bits) -band 31])
        $buffer = $buffer -band ((1 -shl $bits) - 1)
    }
}
if ($bits -gt 0) { [void]$sb.Append($alphabet[($buffer -shl (5 - $bits)) -band 31]) }
$code = ($sb.ToString() -replace '(.{5})(?=.)', '$1-')

Write-Output "Code: $code"
if ($Unlimited) { Write-Output 'Gueltigkeit: unbegrenzt' }
else { Write-Output ("Gueltig bis: {0:yyyy-MM-dd HH:mm} UTC" -f $epoch.AddHours($hours)) }
