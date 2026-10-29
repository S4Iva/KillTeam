# Einmaliger Import der Kill-Team-Daten von Wahapedia (privat, nicht kommerziell).
# Ausgabe: shared/src/commonMain/composeResources/files/killteams.json und files/img/tokens/*.png
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\import-wahapedia.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'shared\src\commonMain\composeResources\files'
$tokDir = Join-Path $res 'img\tokens'
New-Item -ItemType Directory -Force $tokDir | Out-Null
$base = 'https://wahapedia.ru'
. (Join-Path $PSScriptRoot 'paras.ps1')

function Get-Page($url) {
    for ($try = 1; $try -le 5; $try++) {
        Start-Sleep -Milliseconds (800 * $try)
        try { return (Invoke-WebRequest $url -UseBasicParsing -UserAgent 'Mozilla/5.0 KillTeamPrivateImport').Content }
        catch { if ($try -eq 5) { throw } }
    }
}

function Html-ToText($html) {
    $s = [regex]::Replace($html, '<script[\s\S]*?</script>', '')
    $s = [regex]::Replace($s, '<style[\s\S]*?</style>', '')
    $s = [regex]::Replace($s, '<img[^>]*src="([^"]*)"[^>]*>', "`n[IMG:`$1]`n")
    $s = [regex]::Replace($s, '</(div|p|h\d|tr|li|table|td|th)>', "`n")
    $s = [regex]::Replace($s, '<br\s*/?>', "`n")
    $s = [regex]::Replace($s, '<[^>]+>', '')
    $s = [System.Net.WebUtility]::HtmlDecode($s)
    $s = $s -replace [char]0x00A0, ' '
    $s = [regex]::Replace($s, '[ \t]+', ' ')
    $s = [regex]::Replace($s, "(\s*\n\s*)+", "`n")
    $s.Trim()
}

function Strip($html) { (Html-ToText $html) -replace "`n", ' ' }

# Überschriften stehen zuerst im Inhaltsverzeichnis, danach als echter Abschnitt: der zweite Treffer zählt.
function Last-Index($lines, $text) {
    $hits = @(0..($lines.Count - 1) | Where-Object { $lines[$_] -eq $text })
    if ($text -in 'Operatives', 'Marker/Token Guide') { if ($hits.Count -ge 1) { return $hits[0] } else { return -1 } }
    if ($hits.Count -ge 2) { return $hits[1] }
    return -1
}

function Last-Contents($lines) {
    for ($i = $lines.Count - 1; $i -ge 0; $i--) { if ($lines[$i] -eq 'Contents') { return $i } }
    return -1
}

function Is-Name($line) {
    $line.Length -gt 1 -and $line.Length -lt 70 -and $line -ceq $line.ToUpper() -and $line -match '[A-Z]' -and $line -notmatch '^\|' -and $line -notmatch '^\['
}

function Parse-Items($lines) {
    $items = @(); $cur = $null
    foreach ($l in $lines) {
        if ($l.StartsWith('|') -or $l.StartsWith('[IMG')) { continue }
        if (Is-Name $l) {
            if ($cur) { $items += $cur }
            $cur = [ordered]@{ name = $l; text = @() }
        } elseif ($cur) { $cur.text += $l }
    }
    if ($cur) { $items += $cur }
    $items | ForEach-Object { [ordered]@{ name = $_.name; text = ($_.text -join "`n") } }
}

function Get-Region($html, $id) {
    $m = [regex]::Match($html, "<h2 id=`"$id`"[^>]*>([\s\S]*?)(?=<h2 id=|$)")
    if ($m.Success) { $m.Groups[1].Value } else { '' }
}

# Operatives-Abschnitt als Liste: Ebene 1 = arrow, Ebene 2 = bullet, Rest = text.
function Parse-Composition($html) {
    $region = (Get-Region $html 'Operatives') -replace '^[\s\S]*?</h2>', ''
    $script:ulDepth = 0
    $region = [regex]::Replace($region, '<ul[^>]*>|</ul>|<li[^>]*>', {
        param($m)
        if ($m.Value -like '<ul*') { $script:ulDepth++; return "`n" }
        if ($m.Value -eq '</ul>') { $script:ulDepth--; return "`n" }
        if ($script:ulDepth -le 1) { return "`n@@A@@" } else { return "`n@@B@@" }
    })
    $kinds = @{ '@@A@@' = 'arrow'; '@@B@@' = 'bullet' }
    $out = @()
    foreach ($line in ((Html-ToText $region) -split "`n")) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith('[IMG')) { continue }
        $kind = 'text'
        if ($t.Length -ge 5 -and $kinds.ContainsKey($t.Substring(0, 5))) { $kind = $kinds[$t.Substring(0, 5)]; $t = $t.Substring(5).Trim() }
        if ($t) { $out += [ordered]@{ kind = $kind; text = $t } }
    }
    $out
}

function Parse-Strat($html, $id) {
    $items = @()
    $chunks = [regex]::Split((Get-Region $html $id), '<div class=" ?stratWrapper[^"]*">')
    foreach ($chunk in ($chunks | Select-Object -Skip 1)) {
        $nm = [regex]::Match($chunk, '<div class="stratName[^"]*">([\s\S]*?)</div>')
        if (-not $nm.Success) { continue }
        $spans = [regex]::Matches($nm.Groups[1].Value, '<span>([\s\S]*?)</span>')
        $name = if ($spans.Count -gt 0) { Strip $spans[0].Groups[1].Value } else { Strip $nm.Groups[1].Value }
        $cost = if ($spans.Count -gt 1) { Strip $spans[1].Groups[1].Value } else { '' }
        $paras = @(Html-ToParas $chunk.Substring($nm.Index + $nm.Length))
        $items += [ordered]@{ name = $name; cost = $cost; text = (($paras | ForEach-Object { $_.text }) -join "`n"); paras = $paras }
    }
    $items
}

function Parse-FactionRules($html) {
    $region = Get-Region $html 'Faction-Rules'
    $heads = [regex]::Matches($region, '<h3 id="[^"]*">([\s\S]*?)</h3>')
    $items = @()
    for ($i = 0; $i -lt $heads.Count; $i++) {
        $from = $heads[$i].Index + $heads[$i].Length
        $to = if ($i + 1 -lt $heads.Count) { $heads[$i + 1].Index } else { $region.Length }
        $paras = @(Html-ToParas $region.Substring($from, $to - $from))
        $items += [ordered]@{ name = (Strip $heads[$i].Groups[1].Value); cost = ''; text = (($paras | ForEach-Object { $_.text }) -join "`n"); paras = $paras }
    }
    $items
}

function Parse-Block($chunk) {
    $paras = @()
    if ($chunk -match 'h_actions_ds') {
        $hm = [regex]::Match($chunk, '<h3 class="h_actions_ds">([\s\S]*?)</h3>')
        $apm = [regex]::Match($hm.Groups[1].Value, '<span>([\s\S]*?)</span>')
        $ap = if ($apm.Success) { Strip $apm.Groups[1].Value } else { '' }
        $name = Strip ([regex]::Replace($hm.Groups[1].Value, '<span>[\s\S]*?</span>', ''))
        foreach ($m in [regex]::Matches($chunk, '<div class="(action\w+)">([\s\S]*?)</div>')) {
            $kind = switch ($m.Groups[1].Value) { 'actionEffect' { 'effect' } 'actionConditions' { 'condition' } default { 'text' } }
            $paras += [ordered]@{ kind = $kind; text = (Strip $m.Groups[2].Value) }
        }
        return [ordered]@{ kind = 'action'; name = $name; ap = $ap; paras = $paras }
    }
    $name = ''
    $body = $chunk
    $nm = [regex]::Match($chunk, '<span class="redfont">([\s\S]*?)</span>')
    if ($nm.Success) {
        $name = Strip $nm.Groups[1].Value
        $body = [regex]::Replace($chunk.Substring($nm.Index + $nm.Length), '^\s*<b>:\s*</b>', '')
    }
    $dot = [string][char]0x2022
    $body = [regex]::Replace($body, '<li>', "$dot ")
    $lines = @((Html-ToText $body) -split "`n" | Where-Object { $_.Trim() -and -not $_.StartsWith('[IMG') })
    foreach ($l in $lines) {
        if ($l.StartsWith($dot)) { $paras += [ordered]@{ kind = 'bullet'; text = $l.Substring(1).Trim() } }
        else { $paras += [ordered]@{ kind = 'text'; text = $l } }
    }
    [ordered]@{ kind = 'ability'; name = $name; ap = ''; paras = $paras }
}

function Parse-Datacards($html) {
    $parts = $html -split '<div class="dsOuterFrame">'
    $cards = @()
    for ($i = 1; $i -lt $parts.Count; $i++) {
        $p = $parts[$i]
        $nm = [regex]::Match($p, '<h3 id="[^"]*" class="pTable_h3"><div>([^<]*)')
        if (-not $nm.Success) { continue }
        $stats = @{}
        foreach ($m in [regex]::Matches($p, '(APL|MOVE|SAVE|WOUNDS)<div class="dsStat"><img[^>]*>([\s\S]*?)</div>')) {
            $stats[$m.Groups[1].Value.ToLower()] = (Strip $m.Groups[2].Value)
        }
        $weapons = @()
        foreach ($tb in [regex]::Matches($p, '<tbody class=" bkg">([\s\S]*?)</tbody>')) {
            $b = $tb.Groups[1].Value
            $type = if ($b -match 'wsDataMelee') { 'melee' } else { 'ranged' }
            $names = [regex]::Matches($b, '<td class="wTable2_short pad2626"><span>([^<]*)</span>')
            $nums = [regex]::Matches($b, '<div class="ct pad2626">([^<]*)</div>')
            if ($nums.Count -lt 3) { continue }
            $wr = [regex]::Match($b, '<td colspan="2" class="wTable1_long pad2626">([\s\S]*?)</td>')
            if (-not $wr.Success) { $wr = [regex]::Match($b, '<td><span class="wTable1_short pad2626">([\s\S]*?)</span></td>') }
            $name = if ($names.Count -gt 0) { [System.Net.WebUtility]::HtmlDecode($names[0].Groups[1].Value) } else { '' }
            $weapons += [ordered]@{
                name = $name; type = $type
                atk = (Strip $nums[0].Groups[1].Value); hit = (Strip $nums[1].Groups[1].Value); dmg = (Strip $nums[2].Groups[1].Value)
                wr = $(if ($wr.Success) { Strip $wr.Groups[1].Value } else { '-' })
            }
        }
        $wStart = $p.IndexOf('class="wTable"')
        $we = if ($wStart -ge 0) { $p.IndexOf('</table>', $wStart) } else { -1 }
        $kw = $p.IndexOf('<table class="dsKeywords"')
        $rules = @()
        $blocks = @()
        if ($we -ge 0) {
            $abil = if ($kw -gt $we) { $p.Substring($we + 8, $kw - $we - 8) } else { $p.Substring($we + 8) }
            $rules = @((Html-ToText $abil) -split "`n" | Where-Object { $_.Trim() -and -not $_.StartsWith('[IMG') })
            foreach ($chunk in @($abil -split '<div class="BreakInsideAvoid"' | Select-Object -Skip 1)) { $blocks += Parse-Block $chunk }
        }
        $flavor = ''
        $fm = [regex]::Match($p, '<p class="ShowFluff legend2">([\s\S]*?)</p>')
        if ($fm.Success) { $flavor = Strip $fm.Groups[1].Value }
        $baseSize = ''
        $bm = [regex]::Match($p, 'class="ShowBaseSize">([^<]*)<')
        if ($bm.Success) { $bs = [regex]::Match([System.Net.WebUtility]::HtmlDecode($bm.Groups[1].Value), '\d[\dx ]*mm'); if ($bs.Success) { $baseSize = $bs.Value } }
        $keywords = ''
        if ($kw -ge 0) {
            $km = [regex]::Match($p.Substring($kw), 'dsKeywordData">([\s\S]*?)</td>')
            if ($km.Success) { $keywords = Strip $km.Groups[1].Value }
        }
        $cards += [ordered]@{
            name = [System.Net.WebUtility]::HtmlDecode($nm.Groups[1].Value)
            apl = $stats['apl']; move = $stats['move']; save = $stats['save']; wounds = $stats['wounds']
            weapons = $weapons; rules = $rules; keywords = $keywords
            flavor = $flavor; baseSize = $baseSize; blocks = $blocks
        }
    }
    $cards
}

$nav = Get-Page "$base/kill-team3/nav.html"
$slugs = [regex]::Matches($nav, 'href="/kill-team3/kill-teams/([a-z0-9-]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
Write-Output "Teams: $($slugs.Count)"

$teams = @()
$failed = @()
$tokenFiles = @{}
foreach ($slug in $slugs) {
    try {
        $html = Get-Page "$base/kill-team3/kill-teams/$slug"
        $lines = @((Html-ToText $html) -split "`n" | Where-Object { $_.Trim() })
        $cut = Last-Contents $lines
        if ($cut -gt 100) { $lines = $lines[0..($cut - 1)] }
        $titleLine = $lines | Where-Object { $_ -match '\[Kill Team' } | Select-Object -First 1
        $title = ($titleLine -replace '\s*\[Kill Team.*$', '').Trim([char]0xFEFF, ' ')
        $ti = [array]::IndexOf($lines, $titleLine)
        $desc = $lines[$ti + 2]
        if ($desc -eq 'Contents') { $desc = '' }
        $arch = ($lines | Where-Object { $_ -like 'Archetype:*' } | Select-Object -First 1)
        $version = ''; $updated = ''
        for ($vi = 0; $vi -lt [Math]::Min($lines.Count - 3, 80); $vi++) {
            if ($lines[$vi + 1] -eq '3' -and $lines[$vi + 3] -match '^[A-Za-z]+ \d{4}$') { $version = $lines[$vi + 2]; $updated = $lines[$vi + 3]; break }
        }
        $archList = @()
        if ($arch) { $archList = ($arch -replace '^Archetype:\s*', '') -split '\s*/\s*' }

        $iOps = Last-Index $lines 'Operatives'
        $iTok = Last-Index $lines 'Marker/Token Guide'
        $iFR = Last-Index $lines 'Faction Rules'
        $iSP = Last-Index $lines 'Strategy Ploys'
        $iFP = Last-Index $lines 'Firefight Ploys'
        $iEq = Last-Index $lines 'Faction Equipment'
        $iDc = Last-Index $lines 'Datacards'
        $ends = @($iTok, $iFR, $iSP, $iFP, $iEq, $iDc, $lines.Count) | Where-Object { $_ -gt $iOps }
        $compEnd = ($ends | Measure-Object -Minimum).Minimum
        $comp = @(); if ($iOps -ge 0) { $comp = $lines[($iOps + 1)..($compEnd - 1)] }

        function Slice($from, $nexts) {
            if ($from -lt 0) { return @() }
            $to = (@($nexts) + $lines.Count | Where-Object { $_ -gt $from } | Measure-Object -Minimum).Minimum
            if ($to -le $from + 1) { return @() }
            return $lines[($from + 1)..($to - 1)]
        }
        $fr = Slice $iFR @($iSP, $iFP, $iEq, $iDc)
        $sp = Slice $iSP @($iFP, $iEq, $iDc)
        $fp = Slice $iFP @($iEq, $iDc)
        $eq = Slice $iEq @($iDc)

        $tokens = @()
        if ($iTok -ge 0) {
            $tokEnd = (@($iFR, $iSP, $iFP, $iEq, $iDc, $lines.Count) | Where-Object { $_ -gt $iTok } | Measure-Object -Minimum).Minimum
            for ($i = $iTok + 1; $i -lt $tokEnd - 1; $i++) {
                $m = [regex]::Match($lines[$i], '^\[IMG:(/kill-team3/img/tokens/([^\]]+))\]$')
                if ($m.Success) {
                    $file = $m.Groups[2].Value.ToLower() -replace '[^a-z0-9._]', '_'
                    if (-not $tokenFiles.ContainsKey($file)) {
                        $target = Join-Path $tokDir $file
                        if (-not (Test-Path $target)) { Invoke-WebRequest "$base$($m.Groups[1].Value)" -OutFile $target -UseBasicParsing }
                        $tokenFiles[$file] = $true
                    }
                    $tokens += [ordered]@{ name = $lines[$i + 1]; image = $file }
                }
            }
        }

        $teams += [ordered]@{
            slug = $slug; name = $title; description = $desc; archetypes = $archList
            version = $version; updated = $updated
            composition = $comp
            compositionParas = @(Parse-Composition $html)
            factionKeyword = [regex]::Match($html, '<span class="inb fkwb">([^<]*)<').Groups[1].Value
            factionRules = ($fr | Where-Object { -not $_.StartsWith('|') -and -not $_.StartsWith('[IMG') }) -join "`n"
            strategyPloys = @(Parse-Strat $html 'Strategy-Ploys'); firefightPloys = @(Parse-Strat $html 'Firefight-Ploys'); equipment = @(Parse-Strat $html 'Faction-Equipment')
            factionRuleItems = @(Parse-FactionRules $html)
            tokens = $tokens
            operatives = @(Parse-Datacards $html)
        }
        Write-Output "OK $slug ($($teams[-1].operatives.Count) operatives)"
    } catch { Write-Output "FAIL $slug $($_.Exception.Message)"; $failed += $slug }
}

if ($failed.Count -gt 0) { Write-Output "Abbruch, nichts geschrieben. Fehlgeschlagen: $($failed -join ', ')"; exit 1 }
$json = [ordered]@{ source = 'wahapedia.ru/kill-team3'; importedAt = (Get-Date -Format 'yyyy-MM-dd'); teams = $teams } | ConvertTo-Json -Depth 10
[IO.File]::WriteAllText((Join-Path $res 'killteams.json'), $json, (New-Object Text.UTF8Encoding($false)))
Write-Output "Fertig: $($teams.Count) Teams"
