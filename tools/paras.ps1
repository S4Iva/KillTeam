# Gemeinsame Funktion der Importskripte: HTML -> Absaetze mit Art (effect, condition, bullet, flavor, header, text).
# Setzt voraus, dass Html-ToText im aufrufenden Skript definiert ist.
function Html-ToParas($html) {
    $s = $html
    $s = [regex]::Replace($s, '<div class="actionEffect">', "`n@@E@@")
    $s = [regex]::Replace($s, '<div class="actionConditions">', "`n@@C@@")
    $s = [regex]::Replace($s, '<li>', "`n@@B@@")
    $s = [regex]::Replace($s, '<p class="[^"]*ShowFluff[^"]*">', "`n@@F@@")
    $s = [regex]::Replace($s, '<td[^>]*rgb\(241,\s*92,\s*35\)[^>]*>', "`n@@H@@")
    $s = [regex]::Replace($s, '<td[^>]*>\s*<i>', "`n@@F@@")
    $kinds = @{ '@@E@@' = 'effect'; '@@C@@' = 'condition'; '@@B@@' = 'bullet'; '@@F@@' = 'flavor'; '@@H@@' = 'header' }
    $out = @()
    foreach ($line in ((Html-ToText $s) -split "`n")) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith('[IMG')) { continue }
        $kind = 'text'
        if ($t.Length -ge 5 -and $kinds.ContainsKey($t.Substring(0, 5))) { $kind = $kinds[$t.Substring(0, 5)]; $t = $t.Substring(5).Trim() }
        $t = $t.TrimEnd('|').Trim()
        if ($t) { $out += [ordered]@{ kind = $kind; text = $t } }
    }
    $out
}
