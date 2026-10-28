# Prueft, ob Wahapedia fuer die Kill Teams einen neueren Stand zeigt als die in der App gebuendelten Daten.
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\check-updates.ps1
# Danach ggf. neu importieren: tools\import-wahapedia.ps1 (Teams) und tools\import-rules.ps1 (Regeln, Keywords).
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$local = Get-Content (Join-Path $root 'shared\src\commonMain\composeResources\files\killteams.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$base = 'https://wahapedia.ru'

function Get-Version($slug) {
    Start-Sleep -Milliseconds 700
    $h = (Invoke-WebRequest "$base/kill-team3/kill-teams/$slug" -UseBasicParsing -UserAgent 'Mozilla/5.0 KillTeamPrivateImport').Content
    $s = [regex]::Replace($h, '<script[\s\S]*?</script>', '')
    $s = [regex]::Replace($s, '</(td|th|tr|div|p|h\d|li|table)>', "`n")
    $s = [regex]::Replace($s, '<[^>]+>', '')
    $s = [System.Net.WebUtility]::HtmlDecode($s) -replace [char]0x00A0, ' '
    $lines = @($s -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    for ($i = 0; $i -lt [Math]::Min($lines.Count - 3, 120); $i++) {
        if ($lines[$i + 1] -eq '3' -and $lines[$i + 3] -match '^[A-Za-z]+ \d{4}$') { return [pscustomobject]@{ version = $lines[$i + 2]; updated = $lines[$i + 3] } }
    }
    $null
}

Write-Output "Lokaler Datenstand: importiert am $($local.importedAt), $(@($local.teams).Count) Teams"
$changed = @(); $unknown = @()
foreach ($t in $local.teams) {
    $now = Get-Version $t.slug
    if (-not $now) { $unknown += $t.slug; continue }
    if ($now.version -ne $t.version -or $now.updated -ne $t.updated) {
        $changed += "{0,-26} lokal: {1} ({2})  ->  Wahapedia: {3} ({4})" -f $t.slug, $t.version, $t.updated, $now.version, $now.updated
    }
}
if ($changed.Count) { Write-Output "`nNeuer Stand auf Wahapedia:"; $changed } else { Write-Output "`nWahapedia zeigt keinen neueren Stand fuer die Teams." }
if ($unknown.Count) { Write-Output "`nVersion nicht lesbar: $($unknown -join ', ')" }
Write-Output "`nOffizielle Downloads zum Vergleich (Spalte 'Updated'): https://www.warhammer-community.com/en-gb/downloads/kill-team/"
