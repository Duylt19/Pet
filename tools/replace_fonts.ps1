$files = Get-ChildItem -Path "app\src\main\java\com\asianmobile\livetv\ui" -Recurse -Filter "*.kt"
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match "montserrat_bold") {
        $newContent = $content -replace "montserrat_bold", "inter_semibold"
        Set-Content $file.FullName $newContent -NoNewline
        Write-Output "Updated: $($file.Name)"
    }
}
