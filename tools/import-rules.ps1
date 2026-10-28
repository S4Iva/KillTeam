# Einmaliger Import der Regelseiten und Keyword-Definitionen von Wahapedia (privat, nicht kommerziell).
# Ausgabe: shared/src/commonMain/composeResources/files/rules.json und lookup.json
# Aufruf: powershell -ExecutionPolicy Bypass -File tools\import-rules.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'shared\src\commonMain\composeResources\files'
New-Item -ItemType Directory -Force $res | Out-Null
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
    $s = [regex]::Replace($s, '<div class="tooltip_templates">[\s\S]*?</span></div>', '')
    $s = [regex]::Replace($s, '<img[^>]*>', '')
    $s = [regex]::Replace($s, '</(div|p|h\d|tr|li|table)>', "`n")
    $s = [regex]::Replace($s, '</t[dh]>', ' | ')
    $s = [regex]::Replace($s, '<br\s*/?>', "`n")
    $s = [regex]::Replace($s, '<[^>]+>', '')
    $s = [System.Net.WebUtility]::HtmlDecode($s)
    $s = $s -replace [char]0x00A0, ' '
    $s = [regex]::Replace($s, '[ \t]+', ' ')
    $s = [regex]::Replace($s, "(\s*\n\s*)+", "`n")
    $s.Trim()
}

function Strip($html) { ((Html-ToText $html) -replace "`n", ' ').Trim() }

function Normalize($t) {
    $x = $t.ToLower()
    $x = [regex]::Replace($x, '\s+x\+?(\s|$)', ' ')
    $x = [regex]::Replace($x, '[^a-z0-9 ]', '')
    ([regex]::Replace($x, '\s+', ' ')).Trim()
}

function Slug($t) { ([regex]::Replace($t.Trim(), '[^A-Za-z0-9]+', '-')).Trim('-') }

# Balanced scan for <span class="tooltipX" data-tooltip-content="#tooltip_contentX"> ... </span>
function Get-TooltipSurfaces($html) {
    $out = @()
    $start = [regex]::Matches($html, '<span class="tooltip\w+" data-tooltip-content="#tooltip_content(\w+)">')
    foreach ($m in $start) {
        $depth = 1; $pos = $m.Index + $m.Length; $inner = $null
        while ($depth -gt 0) {
            $o = $html.IndexOf('<span', $pos); $c = $html.IndexOf('</span>', $pos)
            if ($c -lt 0) { break }
            if ($o -ge 0 -and $o -lt $c) { $depth++; $pos = $o + 5 }
            else { $depth--; $pos = $c + 7; if ($depth -eq 0) { $inner = $html.Substring($m.Index + $m.Length, $c - ($m.Index + $m.Length)) } }
        }
        if ($inner) { $out += [pscustomobject]@{ id = $m.Groups[1].Value; text = (Strip $inner) } }
    }
    $out
}

function Get-Tooltips($html) {
    $map = @{}
    foreach ($m in [regex]::Matches($html, '<div class="tooltip_templates"><span id="tooltip_content(\w+)">([\s\S]*?)</span></div>')) {
        $body = $m.Groups[2].Value
        $h = [regex]::Match($body, '<div class="tooltip_header">([\s\S]*?)</div>')
        $headerTitle = ''
        $rest = $body
        if ($h.Success) { $headerTitle = Strip $h.Groups[1].Value; $rest = $body.Substring($h.Index + $h.Length) }
        $rest = [regex]::Replace($rest, '<div style="clear:both"></div>', '')
        $link = [regex]::Match($rest, 'href="/kill-team3/the-rules/([a-z0-9-]+)/?#([^"]+)"')
        $map[$m.Groups[1].Value] = [pscustomobject]@{
            title = $headerTitle; text = (Html-ToText $rest)
            page = $(if ($link.Success) { $link.Groups[1].Value } else { $null })
            anchor = $(if ($link.Success) { [System.Net.WebUtility]::HtmlDecode($link.Groups[2].Value) } else { $null })
        }
    }
    $map
}

$nav = Get-Page "$base/kill-team3/nav.html"
$pageList = @()
foreach ($m in [regex]::Matches($nav, 'href="/kill-team3/the-rules/([a-z0-9-]+)"[^>]*>([^<]*)<')) {
    if (-not ($pageList | Where-Object { $_.id -eq $m.Groups[1].Value })) {
        $t = [System.Net.WebUtility]::HtmlDecode($m.Groups[2].Value).Trim()
        if (-not $t) { $t = ($m.Groups[1].Value -replace '-', ' ') }
        $pageList += [pscustomobject]@{ id = $m.Groups[1].Value; title = $t }
    }
}
Write-Output "Pages: $(($pageList | ForEach-Object { $_.id }) -join ', ')"

$sections = @()
$pageHtml = @{}
foreach ($p in $pageList) {
    $html = Get-Page "$base/kill-team3/the-rules/$($p.id)"
    $pageHtml[$p.id] = $html
    $heads = [regex]::Matches($html, '<h([2-4])(?: id="([^"]*)")?[^>]*>([\s\S]*?)</h\1>')
    for ($i = 0; $i -lt $heads.Count; $i++) {
        $h = $heads[$i]
        $title = Strip $h.Groups[3].Value
        $id = [System.Net.WebUtility]::HtmlDecode($h.Groups[2].Value)
        if (-not $title -and -not $id) { continue }
        if (-not $id) { $id = Slug $title }
        if (-not $title) { $title = ($id -replace '-', ' ') }
        $from = $h.Index + $h.Length
        $to = if ($i + 1 -lt $heads.Count) { $heads[$i + 1].Index } else { $html.Length }
        $text = Html-ToText $html.Substring($from, $to - $from)
        $lines = @($text -split "`n" | Where-Object { $_.Trim() })
        $keep = @()
        foreach ($l in $lines) { if ($l -eq 'Contents' -or $l -like '*Vyacheslav Maltsev*') { break }; $keep += $l }
        $paras = @()
        foreach ($pp in @(Html-ToParas $html.Substring($from, $to - $from))) {
            if ($pp.text -eq 'Contents' -or $pp.text -like '*Vyacheslav Maltsev*') { break }
            $paras += $pp
        }
        $sections += [pscustomobject]@{ page = $p.id; id = $id; title = $title; level = [int]$h.Groups[1].Value; text = ($keep -join "`n"); paras = $paras }
    }
    Write-Output "OK $($p.id): $(@($sections | Where-Object { $_.page -eq $p.id }).Count) sections"
}

$sectionIndex = @{}
foreach ($s in $sections) { $k = Normalize $s.title; if ($k -and -not $sectionIndex.ContainsKey($k)) { $sectionIndex[$k] = $s } }
$sectionById = @{}
foreach ($s in $sections) { $sectionById["$($s.page)#$($s.id)"] = $s }

$terms = [ordered]@{}
function Add-Term($phrase, $title, $text, $page, $anchor) {
    $key = $phrase.ToLower()
    if ($phrase.Length -lt 3 -or $phrase.Length -gt 40 -or $terms.Contains($key)) { return }
    $terms[$key] = [ordered]@{ phrase = $phrase; title = $title; text = $text; page = $page; anchor = $anchor }
}

function Harvest($html, $withLinks) {
    $tips = Get-Tooltips $html
    foreach ($s in (Get-TooltipSurfaces $html)) {
        if (-not $tips.ContainsKey($s.id)) { continue }
        $t = $tips[$s.id]
        $tipTitle = if ($t.title) { $t.title } else { $s.text }
        $target = $sectionIndex[(Normalize $tipTitle)]
        $page = $null; $anchor = $null
        if ($target) { $page = $target.page; $anchor = $target.id }
        elseif ($t.page -and $sectionById.ContainsKey("$($t.page)#$($t.anchor)")) { $page = $t.page; $anchor = $t.anchor }
        Add-Term $s.text $tipTitle $t.text $page $anchor
    }
    if ($withLinks) {
        foreach ($m in [regex]::Matches($html, '<a href="/kill-team3/the-rules/([a-z0-9-]+)/?#([^"]+)"[^>]*>([\s\S]*?)</a>')) {
            $page = $m.Groups[1].Value; $anchor = [System.Net.WebUtility]::HtmlDecode($m.Groups[2].Value)
            $sec = $sectionById["$page#$anchor"]
            if (-not $sec) { continue }
            $snippet = $sec.text; if ($snippet.Length -gt 600) { $snippet = $snippet.Substring(0, 600) + '...' }
            Add-Term (Strip $m.Groups[3].Value) $sec.title $snippet $page $anchor
        }
    }
}

foreach ($p in $pageList) { Harvest $pageHtml[$p.id] $true }
Harvest (Get-Page "$base/kill-team3/the-rules/approved-ops-2025/") $true

$slugs = [regex]::Matches($nav, 'href="/kill-team3/kill-teams/([a-z0-9-]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
foreach ($slug in $slugs) {
    try { Harvest (Get-Page "$base/kill-team3/kill-teams/$slug") $false; Write-Output "OK team $slug" }
    catch { Write-Output "FAIL team $slug $($_.Exception.Message)" }
}

$utf8 = New-Object Text.UTF8Encoding($false)
$rulesJson = [ordered]@{ source = 'wahapedia.ru/kill-team3'; importedAt = (Get-Date -Format 'yyyy-MM-dd'); pages = @($pageList); sections = @($sections | Select-Object page, id, title, level, paras) } | ConvertTo-Json -Depth 7
[IO.File]::WriteAllText((Join-Path $res 'rules.json'), $rulesJson, $utf8)
$lookupJson = [ordered]@{ terms = @($terms.Values) } | ConvertTo-Json -Depth 6 -Compress
[IO.File]::WriteAllText((Join-Path $res 'lookup.json'), $lookupJson, $utf8)
Write-Output "Fertig: $($sections.Count) Abschnitte, $($terms.Count) Begriffe"
