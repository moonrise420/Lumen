# Generates the LUMEN boot animation (1280x720) over the Soulride photo, packed STORED for /oem.
# Design: a modern, premium wordmark — wide tracking, cream->teal gradient, thin glow, a growing underline,
# and a staggered letter reveal. Plays the entrance ONCE, then holds on a calm static frame (no pulsing, no loop flicker).
Add-Type -AssemblyName System.Drawing
# Work dir (temp). Put a 16:9 background photo at $sp\sr2\00_oberron.jpg before running.
$sp  = Join-Path $env:TEMP 'lumen-boot'
New-Item -ItemType Directory -Force "$sp\sr2" | Out-Null
$out = "$sp\boot"
Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$out\part0" | Out-Null
New-Item -ItemType Directory -Force "$out\part1" | Out-Null

$W=1280; $H=720; $FPS=24; $N=44
$asset = Join-Path (Split-Path $PSScriptRoot -Parent) 'assets'
function lerpN($a,$b,$t){ return $a + ($b-$a)*$t }
function clamp01($t){ return [Math]::Max(0.0,[Math]::Min(1.0,$t)) }
function ease($t){ $t=clamp01 $t; return 1 - [Math]::Pow(1-$t, 3) }   # ease-out cubic

# geometric font + Soulride mark
$pfc = New-Object System.Drawing.Text.PrivateFontCollection
$pfc.AddFontFile("$asset\fonts\geo.ttf")
$fam = $pfc.Families[0]
$logo = [System.Drawing.Image]::FromFile("$asset\soulride_mark.png")

# background: the Soulride ocean photo (O Berron Da Nave), dimmed
$bgSrc = [System.Drawing.Image]::FromFile("$sp\sr2\00_oberron.jpg")
$bg = New-Object System.Drawing.Bitmap($W,$H)
$bgg = [System.Drawing.Graphics]::FromImage($bg); $bgg.InterpolationMode='HighQualityBicubic'
$sc = [Math]::Max($W/$bgSrc.Width, $H/$bgSrc.Height); $bw=[int]($bgSrc.Width*$sc); $bh=[int]($bgSrc.Height*$sc)
$bgg.DrawImage($bgSrc, [int](($W-$bw)/2), [int](($H-$bh)/2), $bw, $bh)
$bgg.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(205,8,16,18))), 0,0,$W,$H)
$bgg.Dispose(); $bgSrc.Dispose()

# --- lay out "LUMEN" letter by letter with wide tracking, centred on (cx,cy) ---
$chars = @('L','U','M','E','N')
$size  = [single]140
$track = [single]30          # extra space between letters (premium tracking)
$cx=640; $cy=358
$sf = New-Object System.Drawing.StringFormat; $sf.Alignment='Near'; $sf.LineAlignment='Near'

# measure each glyph, build a canonical path positioned at its final place
$glyphs = @(); $widths=@()
foreach ($ch in $chars) {
  $gp = New-Object System.Drawing.Drawing2D.GraphicsPath
  $gp.AddString($ch, $fam, 0, $size, (New-Object System.Drawing.PointF(0,0)), $sf)
  $b = $gp.GetBounds()
  $glyphs += ,$gp; $widths += [single]$b.Width
}
$total = 0.0; foreach ($wd in $widths){ $total += $wd }; $total += $track*($chars.Count-1)
$finalPaths=@(); $x=[single]($cx - $total/2)
for ($j=0; $j -lt $chars.Count; $j++){
  $gp=$glyphs[$j]; $b=$gp.GetBounds()
  $m = New-Object System.Drawing.Drawing2D.Matrix
  $m.Translate([single]($x - $b.Left), [single]($cy - $b.Top - $b.Height/2))
  $p2=$gp.Clone(); $p2.Transform($m)
  $finalPaths += ,$p2
  $x += $widths[$j] + $track
}
$markW=120; $markH=[int]($logo.Height*$markW/$logo.Width)
$markRect = New-Object System.Drawing.Rectangle(($cx-[int]($markW/2)), 150, $markW, $markH)
$underY = [int]($cy + $size*0.42)

# draw one composed frame onto $g. letterA = per-letter alpha(0..1), letterRise px, sweepP (0..1 or -1), underFrac, markA
function drawFrame($g, [double[]]$letterA, [double[]]$letterRise, [double]$sweepP, [double]$underFrac, [double]$markA) {
  $g.SmoothingMode='AntiAlias'; $g.InterpolationMode='HighQualityBicubic'; $g.TextRenderingHint='AntiAlias'
  $g.DrawImage($bg,0,0,$W,$H)

  # Soulride mark, fading in above
  if ($markA -gt 0.01) {
    $ia = New-Object System.Drawing.Imaging.ImageAttributes
    $cm = New-Object System.Drawing.Imaging.ColorMatrix; $cm.Matrix33=[single]($markA*0.9); $ia.SetColorMatrix($cm)
    $g.DrawImage($logo, $markRect, 0,0,$logo.Width,$logo.Height, 'Pixel', $ia)
  }

  # gradient brush for the letters (cream top -> soft Soulride teal bottom)
  $gr = New-Object System.Drawing.Rectangle(0, ($cy-[int]($size*0.6)), $W, [int]($size*1.3))
  for ($j=0; $j -lt $finalPaths.Count; $j++) {
    $a = clamp01 $letterA[$j]
    if ($a -le 0.01) { continue }
    $rise = [single]$letterRise[$j]
    $mm = New-Object System.Drawing.Drawing2D.Matrix; $mm.Translate(0,$rise)
    $pp = $finalPaths[$j].Clone(); $pp.Transform($mm)

    # soft outer glow (expanding translucent teal outlines)
    foreach ($gw in @(9,6,3)) {
      $ga=[int](28*$a); if ($ga -gt 0) {
        $pen=New-Object System.Drawing.Pen((New-Object System.Drawing.Color),[single]$gw)
        $pen.Color=[System.Drawing.Color]::FromArgb($ga,143,211,193); $pen.LineJoin='Round'
        $g.DrawPath($pen,$pp)
      }
    }
    # soft drop shadow for a touch of depth
    $sh=New-Object System.Drawing.Drawing2D.Matrix; $sh.Translate([single]2,[single]3)
    $ps=$pp.Clone(); $ps.Transform($sh)
    $g.FillPath((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb([int](90*$a),0,0,0))), $ps)

    # gradient face
    $lg=New-Object System.Drawing.Drawing2D.LinearGradientBrush($gr, ([System.Drawing.Color]::FromArgb([int](255*$a),244,239,230)), ([System.Drawing.Color]::FromArgb([int](255*$a),175,220,205)), 90.0)
    $g.FillPath($lg,$pp)
    # crisp thin edge
    $edge=New-Object System.Drawing.Pen(([System.Drawing.Color]::FromArgb([int](120*$a),255,255,255)),[single]1.2); $edge.LineJoin='Round'
    $g.DrawPath($edge,$pp)
  }

  # growing underline accent (teal), centred
  if ($underFrac -gt 0) {
    $uw=[int]($total*0.5*$underFrac); if ($uw -gt 1) {
      $ur=New-Object System.Drawing.Rectangle(($cx-$uw),$underY,($uw*2),3)
      $ug=New-Object System.Drawing.Drawing2D.LinearGradientBrush($ur, ([System.Drawing.Color]::FromArgb(0,143,211,193)), ([System.Drawing.Color]::FromArgb(230,143,211,193)), 0.0)
      $ub=New-Object System.Drawing.Drawing2D.ColorBlend(3)
      $ub.Colors=@(([System.Drawing.Color]::FromArgb(0,143,211,193)),([System.Drawing.Color]::FromArgb(235,185,230,215)),([System.Drawing.Color]::FromArgb(0,143,211,193)))
      $ub.Positions=@([single]0.0,[single]0.5,[single]1.0); $ug.InterpolationColors=$ub
      $g.FillRectangle($ug,$ur)
    }
  }

  # single light sweep across the letters (entrance only)
  if ($sweepP -ge 0) {
    $clip=New-Object System.Drawing.Drawing2D.GraphicsPath
    for ($j=0; $j -lt $finalPaths.Count; $j++){ $clip.AddPath($finalPaths[$j],$false) }
    $g.SetClip($clip)
    $sweepX=[int](lerpN -300 1580 $sweepP); $bandW=170
    $lr=New-Object System.Drawing.Rectangle(($sweepX-$bandW),0,($bandW*2),$H)
    $lg2=New-Object System.Drawing.Drawing2D.LinearGradientBrush($lr, ([System.Drawing.Color]::FromArgb(0,255,255,255)), ([System.Drawing.Color]::FromArgb(0,255,255,255)), 0.0)
    $bl=New-Object System.Drawing.Drawing2D.ColorBlend(3)
    $bl.Colors=@(([System.Drawing.Color]::FromArgb(0,255,255,255)),([System.Drawing.Color]::FromArgb(150,255,255,255)),([System.Drawing.Color]::FromArgb(0,255,255,255)))
    $bl.Positions=@([single]0.0,[single]0.5,[single]1.0); $lg2.InterpolationColors=$bl
    $g.FillRectangle($lg2,$lr); $g.ResetClip()
  }
}

# --- part0: entrance, played ONCE ---
for ($i=0; $i -lt $N; $i++) {
  $p = $i/[double]($N-1)
  $bmp=New-Object System.Drawing.Bitmap($W,$H); $g=[System.Drawing.Graphics]::FromImage($bmp)
  $la=@(); $lr=@()
  for ($j=0; $j -lt 5; $j++){
    $start=0.06*$j; $lp=ease (($p-$start)/0.45)   # staggered per letter
    $la += [double]$lp
    $lr += [double](lerpN 26 0 $lp)
  }
  $markA = ease(($p-0.12)/0.5)
  $underFrac = ease(($p-0.45)/0.5)
  $sweepP = if ($p -ge 0.55) { clamp01 (($p-0.55)/0.4) } else { -1 }
  drawFrame $g $la $lr $sweepP $underFrac $markA
  $bmp.Save(("$out\part0\{0:0000}.png" -f ($i+1)), [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose(); $bmp.Dispose()
}

# --- part1: calm static hold, looped (settled wordmark, no motion) ---
$bmp=New-Object System.Drawing.Bitmap($W,$H); $g=[System.Drawing.Graphics]::FromImage($bmp)
drawFrame $g @(1,1,1,1,1) @(0,0,0,0,0) -1 1.0 1.0
$bmp.Save("$out\part1\0001.png",[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()

$logo.Dispose(); $bg.Dispose()

# desc: play entrance once, then hold on the static frame forever
"$W $H $FPS`np 1 0 part0`np 0 0 part1`n" | Out-File -Encoding ascii -NoNewline "$out\desc.txt"

$zip="$sp\lumen-bootanimation.zip"
Remove-Item $zip -ErrorAction SilentlyContinue
Add-Type -AssemblyName System.IO.Compression; Add-Type -AssemblyName System.IO.Compression.FileSystem
$fs=[System.IO.File]::Open($zip,'Create'); $za=New-Object System.IO.Compression.ZipArchive($fs,'Create')
function addStored($arch,$file,$name){ $e=$arch.CreateEntry($name,[System.IO.Compression.CompressionLevel]::NoCompression); $es=$e.Open(); $b=[System.IO.File]::ReadAllBytes($file); $es.Write($b,0,$b.Length); $es.Close() }
addStored $za "$out\desc.txt" "desc.txt"
Get-ChildItem "$out\part0" -Filter *.png | Sort Name | % { addStored $za $_.FullName ("part0/"+$_.Name) }
Get-ChildItem "$out\part1" -Filter *.png | Sort Name | % { addStored $za $_.FullName ("part1/"+$_.Name) }
$za.Dispose(); $fs.Close()
"frames: $N + 1 static  zip: {0:N0} KB" -f ((Get-Item $zip).Length/1KB)
