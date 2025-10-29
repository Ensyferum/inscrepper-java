# Script para exibir todos os dados coletados do perfil oncallpeds

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DADOS COLETADOS - @oncallpeds" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Aguardar aplicação estar pronta
Write-Host "Aguardando aplicação iniciar..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/profiles/username/oncallpeds/contents" -ErrorAction Stop
    $data = $response.Content | ConvertFrom-Json
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "PERFIL: @$($data.profile.username)" -ForegroundColor Green
    Write-Host "Nome: $($data.profile.displayName)" -ForegroundColor Green
    Write-Host "Total de Posts Coletados: $($data.totalContents)" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    
    $counter = 1
    foreach ($content in $data.contents) {
        Write-Host "----------------------------------------" -ForegroundColor Magenta
        Write-Host "POST #$counter" -ForegroundColor Magenta
        Write-Host "----------------------------------------" -ForegroundColor White
        Write-Host "ID: $($content.id)" -ForegroundColor White
        Write-Host "External ID: $($content.externalId)" -ForegroundColor White
        Write-Host "Tipo: $($content.type)" -ForegroundColor White
        Write-Host "URL: $($content.url)" -ForegroundColor Cyan
        Write-Host "Media URL: $($content.mediaUrl)" -ForegroundColor Cyan
        Write-Host "Publicado em: $($content.publishedAt)" -ForegroundColor Yellow
        Write-Host "Coletado em: $($content.collectedAt)" -ForegroundColor Yellow
        Write-Host "Tem Imagem: $($content.hasImage)" -ForegroundColor $(if ($content.hasImage) { "Green" } else { "Red" })
        Write-Host "Tamanho da Imagem: $($content.imageSizeKB) KB" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "CAPTION:" -ForegroundColor Green
        Write-Host "$($content.caption)" -ForegroundColor White
        Write-Host ""
        $counter++
    }
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "ESTATÍSTICAS" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    $postsComCaption = ($data.contents | Where-Object { $_.caption -and $_.caption.Trim() -ne "" }).Count
    $postsComImagem = ($data.contents | Where-Object { $_.hasImage }).Count
    $tamanhoTotalKB = ($data.contents | Measure-Object -Property imageSizeKB -Sum).Sum
    
    Write-Host "Posts com caption: $postsComCaption / $($data.totalContents)" -ForegroundColor Green
    Write-Host "Posts com imagem: $postsComImagem / $($data.totalContents)" -ForegroundColor Green
    Write-Host "Tamanho total das imagens: $tamanhoTotalKB KB ($([math]::Round($tamanhoTotalKB/1024, 2)) MB)" -ForegroundColor Yellow
    Write-Host ""
    
} catch {
    Write-Host "ERRO: Não foi possível conectar à aplicação." -ForegroundColor Red
    Write-Host "Certifique-se de que a aplicação está rodando em http://localhost:8080" -ForegroundColor Red
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}
