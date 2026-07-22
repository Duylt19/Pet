<#
.SYNOPSIS
    Export SVG icons from Figma MCP server and convert to Android Vector Drawable XML.

.DESCRIPTION
    This script downloads SVG content from Figma's localhost asset server,
    converts it to Android VectorDrawable XML using svg2vectordrawable (s2v),
    and saves it to the project's res/drawable directory.

.PARAMETER SvgUrl
    The localhost URL of the SVG asset from Figma MCP (e.g., http://localhost:3845/assets/xxx.svg)

.PARAMETER OutputName
    The output filename (without extension). Will be saved as ic_{name}.xml
    Must follow Android naming: lowercase, underscores only.

.PARAMETER DrawablePath
    [Optional] Path to the drawable directory. Defaults to the project's res/drawable.

.PARAMETER TintColor
    [Optional] Tint color to apply (e.g., "#FFFFFF"). If not set, original colors are preserved.

.EXAMPLE
    .\figma_icon_export.ps1 -SvgUrl "http://localhost:3845/assets/abc.svg" -OutputName "tab_channel"
    # Output: app/src/main/res/drawable/ic_tab_channel.xml

.EXAMPLE
    .\figma_icon_export.ps1 -SvgUrl "http://localhost:3845/assets/abc.svg" -OutputName "tab_channel" -TintColor "#FB2C36"
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$SvgUrl,

    [Parameter(Mandatory=$true)]
    [string]$OutputName,

    [string]$DrawablePath = "",

    [string]$TintColor = ""
)

$ErrorActionPreference = "Stop"

# Resolve drawable path
$ProjectRoot = (Get-Item "$PSScriptRoot\..\..\..\..\").FullName
if ([string]::IsNullOrEmpty($DrawablePath)) {
    $DrawablePath = Join-Path $ProjectRoot "app\src\main\res\drawable"
}

# Ensure output directory exists
if (!(Test-Path $DrawablePath)) {
    New-Item -ItemType Directory -Path $DrawablePath -Force | Out-Null
}

# Create temp directory for SVG files
$TempDir = Join-Path $ProjectRoot "tools\temp_svg"
if (!(Test-Path $TempDir)) {
    New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
}

$SvgFile = Join-Path $TempDir "$OutputName.svg"
$XmlFile = Join-Path $DrawablePath "ic_$OutputName.xml"

Write-Host "=== Figma Icon Export ===" -ForegroundColor Cyan
Write-Host "URL: $SvgUrl"
Write-Host "Output: ic_$OutputName.xml"

# Step 1: Download SVG
Write-Host "`n[1/3] Downloading SVG..." -ForegroundColor Yellow
try {
    $svgContent = Invoke-WebRequest -Uri $SvgUrl -UseBasicParsing | Select-Object -ExpandProperty Content
    
    # Fix SVG: remove CSS var() fills like fill="var(--fill-0, #FB2C36)" -> fill="#FB2C36"
    $svgContent = $svgContent -replace 'fill="var\(--fill-\d+,\s*([^)]+)\)"', 'fill="$1"'
    $svgContent = $svgContent -replace 'stroke="var\(--stroke-\d+,\s*([^)]+)\)"', 'stroke="$1"'
    
    # Fix SVG: remove preserveAspectRatio="none" and percentage width/height
    $svgContent = $svgContent -replace 'preserveAspectRatio="none"', ''
    $svgContent = $svgContent -replace 'width="100%"', ''
    $svgContent = $svgContent -replace 'height="100%"', ''
    $svgContent = $svgContent -replace 'overflow="visible"', ''
    $svgContent = $svgContent -replace 'style="display:\s*block;"', ''
    
    Set-Content -Path $SvgFile -Value $svgContent -Encoding UTF8
    Write-Host "  Downloaded: $SvgFile" -ForegroundColor Green
} catch {
    Write-Error "Failed to download SVG: $_"
    exit 1
}

# Step 2: Convert SVG to VectorDrawable XML
Write-Host "[2/3] Converting to VectorDrawable..." -ForegroundColor Yellow
try {
    $s2vArgs = "-i `"$SvgFile`" -o `"$XmlFile`""
    if (![string]::IsNullOrEmpty($TintColor)) {
        $s2vArgs += " -t `"$TintColor`""
    }
    $result = cmd /c "s2v $s2vArgs 2>&1"
    Write-Host "  Converted: $XmlFile" -ForegroundColor Green
} catch {
    Write-Error "Failed to convert SVG: $_"
    exit 1
}

# Step 3: Post-process - ensure Android compatibility
Write-Host "[3/3] Post-processing..." -ForegroundColor Yellow
if (Test-Path $XmlFile) {
    $xmlContent = Get-Content $XmlFile -Raw
    
    # Verify it's a valid VectorDrawable
    if ($xmlContent -match "android:viewportWidth") {
        Write-Host "  Valid VectorDrawable generated!" -ForegroundColor Green
    } else {
        Write-Host "  WARNING: Output may not be a valid VectorDrawable" -ForegroundColor Red
    }
    
    # Show preview
    Write-Host "`n--- Output Preview ---" -ForegroundColor Cyan
    Get-Content $XmlFile | Select-Object -First 15
    Write-Host "..."
    
    Write-Host "`n=== SUCCESS ===" -ForegroundColor Green
    Write-Host "File saved: $XmlFile"
} else {
    Write-Error "Output file was not created!"
    exit 1
}

# Clean up temp SVG
Remove-Item $SvgFile -Force -ErrorAction SilentlyContinue
