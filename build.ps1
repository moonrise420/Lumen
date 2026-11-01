# Builds Lumen without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Usage: pwsh -File build.ps1 [-Install] [-System]
#   -System : sharedUserId=android.uid.system + REBOOT permission, signed with the AOSP platform key
#             (firmware that trusts the platform key runs the app with system privileges: live zoom, reboot, ...).
#
# Signing keys are NOT included in this source release. Provide your own before building:
#   keystore\lumen.jks      + keystore\password.txt   (one line: the keystore password)  -> normal build
#   platformkey\platform.pk8 + platformkey\platform.x509.pem                              -> -System build
# For a system build the key must be the platform key your device's firmware trusts
# (on many AOSP-based boxes that is the public AOSP test platform key).
#
# Device for -Install: set the env var LUMEN_DEVICE to "ip:5555" (networked) or a serial,
# or leave it empty to use the single attached adb device.
param([switch]$Install, [switch]$System)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$sdk  = "$env:LOCALAPPDATA\Android\Sdk"
$bt   = "$sdk\build-tools\37.0.0"
$jar  = "$sdk\platforms\android-36\android.jar"
$jdk  = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin'
$out  = "$here\build"
$ks   = "$here\keystore\lumen.jks"
$adb  = "$sdk\platform-tools\adb.exe"
$dev  = $env:LUMEN_DEVICE   # e.g. "192.168.1.50:5555" or a serial; empty = the only attached device
$devArg = if ($dev) { @('-s', $dev) } else { @() }

Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$out\res", "$out\gen", "$out\classes", "$out\dex" | Out-Null

# Pick the manifest. For -System, inject sharedUserId + REBOOT into a temp copy.
$manifest = "$here\AndroidManifest.xml"
if ($System) {
    $m = Get-Content $manifest -Raw
    $m = $m -replace '(package="de\.lumen\.home")', '$1 android:sharedUserId="android.uid.system"'
    $m = $m -replace '(<uses-permission android:name="android\.permission\.REQUEST_DELETE_PACKAGES" />)', "`$1`n    <uses-permission android:name=`"android.permission.REBOOT`" />`n    <uses-permission android:name=`"android.permission.DEVICE_POWER`" />"
    $manifest = "$out\AndroidManifest.system.xml"
    Set-Content $manifest $m -Encoding UTF8
}

& "$bt\aapt2.exe" compile --dir "$here\res" -o "$out\res.zip"
if ($LASTEXITCODE) { throw 'aapt2 compile failed' }
$assetArg = @(); if (Test-Path "$here\assets") { $assetArg = @('-A', "$here\assets") }
& "$bt\aapt2.exe" link -I $jar --manifest $manifest -o "$out\base.apk" "$out\res.zip" @assetArg --java "$out\gen" --min-sdk-version 30 --target-sdk-version 30
if ($LASTEXITCODE) { throw 'aapt2 link failed' }

$srcFiles = Get-ChildItem "$here\src", "$out\gen" -Recurse -Filter *.java | ForEach-Object FullName
& "$jdk\javac.exe" -encoding UTF-8 --release 8 -classpath $jar -d "$out\classes" @srcFiles
if ($LASTEXITCODE) { throw 'javac failed' }

$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --release --min-api 30 --lib $jar --output "$out\dex" @classes
if ($LASTEXITCODE) { throw 'd8 failed' }

Copy-Item "$out\base.apk" "$out\unsigned.apk"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::Open("$out\unsigned.apk", 'Update')
[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($z, "$out\dex\classes.dex", 'classes.dex') | Out-Null
$z.Dispose()

& "$bt\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw 'zipalign failed' }

if ($System) {
    $pk = "$here\platformkey"
    if (-not (Test-Path "$pk\platform.pk8")) { throw "Missing platform key. Put platform.pk8 + platform.x509.pem in $pk (see header)." }
    & "$bt\apksigner.bat" sign --key "$pk\platform.pk8" --cert "$pk\platform.x509.pem" --out "$out\Lumen.apk" "$out\aligned.apk"
} else {
    if (-not (Test-Path $ks)) { throw "Missing keystore. Put lumen.jks + password.txt in $here\keystore (see header)." }
    $pw = Get-Content "$here\keystore\password.txt" -Raw
    & "$bt\apksigner.bat" sign --ks $ks --ks-pass "pass:$($pw.Trim())" --out "$out\Lumen.apk" "$out\aligned.apk"
}
if ($LASTEXITCODE) { throw 'apksigner failed' }
"Built $out\Lumen.apk  ({0:N0} KB){1}" -f (((Get-Item "$out\Lumen.apk").Length / 1KB), $(if ($System) { '  [SYSTEM]' } else { '' }))

if ($Install) {
    if ($System) {
        # sharedUserId can't change on update: remove the normal Lumen first.
        & $adb @devArg uninstall de.lumen.home 2>&1 | Out-Null
        & $adb @devArg install "$out\Lumen.apk"
        Start-Sleep 3
        & $adb @devArg shell "cmd package set-home-activity de.lumen.home/.HomeActivity; settings put secure enabled_accessibility_services de.lumen.home/.RemoteService; settings put secure accessibility_enabled 1"
    } else {
        & $adb @devArg install -r "$out\Lumen.apk"
        Start-Sleep 30
        & $adb @devArg shell "settings put secure enabled_accessibility_services de.lumen.home/.RemoteService; settings put secure accessibility_enabled 1"
    }
}
