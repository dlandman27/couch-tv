param(
    [string]$Text,
    [switch]$Clear,
    # Emulator only. Never point this at the real Fire TV (10.0.0.207:5555).
    [string]$Serial = "emulator-5554"
)
# Types $Text on Orbit's on-screen search keyboard with D-pad presses, starting with focus on "A".
# Rows: ABCDEF / GHIJKL / MNOPQR / STUVWX / YZ1234 / 567890, then [backspace][Space][Clear][Search].
# Moves are batched; every OK is sent alone with a pause, or the emulator drops some.
if ($Serial -like "10.0.0.207*") { throw "Refusing to drive the real TV. Use the emulator." }
$adb = "C:\Users\dylan\adb\platform-tools\adb.exe"
$rows = @("abcdef", "ghijkl", "mnopqr", "stuvwx", "yz1234", "567890")
$script:r = 0; $script:c = 0
$script:moves = New-Object System.Collections.Generic.List[string]
function Flush { if ($script:moves.Count -gt 0) { & $adb -s $Serial shell input keyevent ($script:moves -join " ").Split(" "); $script:moves.Clear(); Start-Sleep -Milliseconds 300 } }
function Ok { Flush; & $adb -s $Serial shell input keyevent 23; Start-Sleep -Milliseconds 450 }
function Go($tr, $tc) {
    while ($script:r -lt $tr) { $script:moves.Add("20"); $script:r++ }
    while ($script:r -gt $tr) { $script:moves.Add("19"); $script:r-- }
    while ($script:c -lt $tc) { $script:moves.Add("22"); $script:c++ }
    while ($script:c -gt $tc) { $script:moves.Add("21"); $script:c-- }
}
function Home { 1..6 | ForEach-Object { $script:moves.Add("19") }; 1..6 | ForEach-Object { $script:moves.Add("21") }; Flush; $script:r = 0; $script:c = 0 }
if ($Clear) { Go 5 0; $script:moves.Add("20"); $script:moves.Add("22"); $script:moves.Add("22"); Ok; Home }   # Clear key
foreach ($ch in $Text.ToLower().ToCharArray()) {
    if ($ch -eq ' ') { Go 5 0; $script:moves.Add("20"); $script:moves.Add("22"); Ok; Home; continue }   # Space key
    for ($i = 0; $i -lt 6; $i++) { $j = $rows[$i].IndexOf($ch); if ($j -ge 0) { Go $i $j; Ok; break } }
}
Flush
