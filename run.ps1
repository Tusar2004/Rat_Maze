# Run script for Algorithm Rat — Pathfinding Visualizer
Write-Host "Compiling..." -ForegroundColor Cyan
javac -d out -sourcepath src (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)

if ($LASTEXITCODE -eq 0) {
    Write-Host "Starting game..." -ForegroundColor Green
    java -cp out main.MainFrame
} else {
    Write-Host "Compilation failed. Fix errors above." -ForegroundColor Red
}
