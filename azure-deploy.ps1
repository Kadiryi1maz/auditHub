param(
  [string]$ResourceGroupName = "audithub-rg",
  [string]$StorageAccountName = "audithubstaticstorage",
  [string]$Location = "westeurope"
)

Write-Host "Packaging site..."
$zipPath = Join-Path -Path $PSScriptRoot -ChildPath 'audithub-static.zip'
if(-Not (Test-Path $zipPath)){
  Write-Host "audithub-static.zip bulunamadı, oluşturuluyor."
  Compress-Archive -Path (Join-Path $PSScriptRoot 'index.html'), (Join-Path $PSScriptRoot 'data'), (Join-Path $PSScriptRoot 'wwwroot') -DestinationPath $zipPath -Force
}

Write-Host "Deploying to Azure Static Website (requires Azure CLI and az login)..."
az storage account create --name $StorageAccountName --resource-group $ResourceGroupName --location $Location --sku Standard_LRS 2>$null | Out-Null
az storage blob upload-batch -s $PSScriptRoot -d "\$web" --account-name $StorageAccountName --pattern "index.html" 2>$null
az storage blob upload-batch -s $PSScriptRoot\data -d "\$web\data" --account-name $StorageAccountName 2>$null

$staticUrl = "https://$StorageAccountName.z6.web.core.windows.net"
Write-Host "Deployed. Site URL: $staticUrl"