<#
.SYNOPSIS
    Installs the latest Old Animations build into a Fabric mods folder.

.DESCRIPTION
    Downloads the newest oldanimations-*.jar from the GitHub releases page,
    removes any older copy from the target mods folder, and drops the new one
    in. Remembers the folder it used so later runs are a single command.

.PARAMETER ModsDir
    Mods folder to install into. Only needed the first time, or to change it.
    Saved to %LOCALAPPDATA%\OldAnimations\install.json afterwards.

.PARAMETER Version
    Install a specific release tag (e.g. v1.0.1) instead of the latest.

.PARAMETER ListDirs
    Show every mods folder that was detected and exit without installing.

.PARAMETER Force
    Reinstall even when the installed version already matches.

.EXAMPLE
    .\install-oldanimations.ps1
    .\install-oldanimations.ps1 -ListDirs
    .\install-oldanimations.ps1 -ModsDir "$env:USERPROFILE\.lunarclient\profiles\default\26.2\mods"
#>

[CmdletBinding()]
param(
    [string] $ModsDir,
    [string] $Version,
    [switch] $ListDirs,
    [switch] $Force
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$Repo      = 'lukasbaxter/old-animations'
$JarPrefix = 'oldanimations-'
$StateFile = Join-Path $env:LOCALAPPDATA 'OldAnimations\install.json'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok  ($msg) { Write-Host "    $msg" -ForegroundColor Green }
function Write-Warn2($msg){ Write-Host "    $msg" -ForegroundColor Yellow }

# --- find candidate mods folders -----------------------------------------

function Get-CandidateModsDirs {
    $candidates = [System.Collections.Generic.List[string]]::new()

    # Lunar Client keeps per-profile, per-version external mod folders.
    $lunarProfiles = Join-Path $env:USERPROFILE '.lunarclient\profiles'
    if (Test-Path $lunarProfiles) {
        Get-ChildItem $lunarProfiles -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            Get-ChildItem $_.FullName -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $mods = Join-Path $_.FullName 'mods'
                if (Test-Path $mods) { $candidates.Add($mods) }
            }
        }
    }

    # Vanilla launcher / MultiMC-style .minecraft
    $dotMc = Join-Path $env:APPDATA '.minecraft\mods'
    if (Test-Path $dotMc) { $candidates.Add($dotMc) }

    # Prism / MultiMC instances
    foreach ($root in @(
        (Join-Path $env:APPDATA 'PrismLauncher\instances'),
        (Join-Path $env:APPDATA 'MultiMC\instances')
    )) {
        if (Test-Path $root) {
            Get-ChildItem $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $mods = Join-Path $_.FullName '.minecraft\mods'
                if (Test-Path $mods) { $candidates.Add($mods) }
            }
        }
    }

    $candidates | Sort-Object -Unique
}

function Resolve-ModsDir {
    if ($ModsDir) {
        if (-not (Test-Path $ModsDir)) {
            Write-Step "Creating $ModsDir"
            New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
        }
        return (Resolve-Path $ModsDir).Path
    }

    if (Test-Path $StateFile) {
        $saved = (Get-Content $StateFile -Raw | ConvertFrom-Json).modsDir
        if ($saved -and (Test-Path $saved)) { return $saved }
        Write-Warn2 "Saved folder '$saved' is gone; re-detecting."
    }

    $found = @(Get-CandidateModsDirs)
    if ($found.Count -eq 0) {
        throw "No mods folder found. Pass one explicitly, e.g.`n" +
              "  .\install-oldanimations.ps1 -ModsDir `"`$env:USERPROFILE\.lunarclient\profiles\default\26.2\mods`""
    }
    if ($found.Count -eq 1) {
        Write-Ok "Detected mods folder: $($found[0])"
        return $found[0]
    }

    Write-Host "`nSeveral mods folders were found:`n"
    for ($i = 0; $i -lt $found.Count; $i++) { Write-Host ("  [{0}] {1}" -f $i, $found[$i]) }
    $choice = Read-Host "`nWhich one? (0-$($found.Count - 1))"
    if ($choice -notmatch '^\d+$' -or [int]$choice -ge $found.Count) { throw "Not a valid choice." }
    return $found[[int]$choice]
}

# --- release lookup -------------------------------------------------------

function Get-Release {
    $url = if ($Version) {
        "https://api.github.com/repos/$Repo/releases/tags/$Version"
    } else {
        "https://api.github.com/repos/$Repo/releases/latest"
    }

    $headers = @{ 'User-Agent' = 'old-animations-installer'; 'Accept' = 'application/vnd.github+json' }
    if ($env:GITHUB_TOKEN) { $headers['Authorization'] = "Bearer $env:GITHUB_TOKEN" }

    try {
        Invoke-RestMethod -Uri $url -Headers $headers -TimeoutSec 30
    } catch {
        throw "Could not reach the GitHub releases API for $Repo. $($_.Exception.Message)"
    }
}

# --- main -----------------------------------------------------------------

if ($ListDirs) {
    Write-Step 'Detected mods folders'
    $dirs = @(Get-CandidateModsDirs)
    if ($dirs.Count -eq 0) { Write-Warn2 'None found.' } else { $dirs | ForEach-Object { Write-Host "    $_" } }
    return
}

$target = Resolve-ModsDir
Write-Step "Target: $target"

Write-Step 'Looking up release'
$release = Get-Release
$asset = $release.assets | Where-Object { $_.name -like "$JarPrefix*.jar" } | Select-Object -First 1
if (-not $asset) { throw "Release '$($release.tag_name)' has no $JarPrefix*.jar asset attached." }
Write-Ok "$($release.tag_name) -> $($asset.name) ($([math]::Round($asset.size / 1KB)) KB)"

$existing = @(Get-ChildItem $target -Filter "$JarPrefix*.jar" -File -ErrorAction SilentlyContinue)
if (-not $Force -and $existing.Count -eq 1 -and $existing[0].Name -eq $asset.name) {
    Write-Ok "$($asset.name) is already installed. Use -Force to reinstall."
    return
}

# Download to a temp file first so a failed download cannot leave the mods
# folder with no jar in it.
$temp = Join-Path ([System.IO.Path]::GetTempPath()) $asset.name
Write-Step 'Downloading'
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $temp -TimeoutSec 120 -UseBasicParsing
$size = (Get-Item $temp).Length
if ($size -ne $asset.size) {
    Remove-Item $temp -Force -ErrorAction SilentlyContinue
    throw "Download is $size bytes but the release says $($asset.size). Aborting rather than installing a truncated jar."
}
Write-Ok "Downloaded $([math]::Round($size / 1KB)) KB"

if ($existing.Count -gt 0) {
    Write-Step 'Removing old build(s)'
    foreach ($old in $existing) {
        try {
            Remove-Item $old.FullName -Force
            Write-Ok "Removed $($old.Name)"
        } catch {
            throw "Could not delete $($old.Name). Close Minecraft/Lunar Client and run this again."
        }
    }
}

Write-Step 'Installing'
try {
    Move-Item -Path $temp -Destination (Join-Path $target $asset.name) -Force
} catch {
    throw "Could not write into $target. Close Minecraft/Lunar Client and run this again. $($_.Exception.Message)"
}
Write-Ok "Installed $($asset.name)"

New-Item -ItemType Directory -Path (Split-Path $StateFile) -Force | Out-Null
@{ modsDir = $target; version = $release.tag_name } | ConvertTo-Json | Set-Content $StateFile -Encoding UTF8

Write-Host "`nDone. Launch Minecraft 26.2 with Fabric and press O for the settings screen.`n" -ForegroundColor Green
