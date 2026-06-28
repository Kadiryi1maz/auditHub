Param(
  [string]$GitHubUrl
)

if(-not $GitHubUrl){
  $GitHubUrl = Read-Host "Enter GitHub repository URL (https://github.com/username/repo.git)"
}

Write-Host "Adding remote 'github' -> $GitHubUrl"
git remote remove github -ErrorAction SilentlyContinue
git remote add github $GitHubUrl

Write-Host "Pushing to github master..."
git push github master

Write-Host "Done. If push failed, check remote URL and your GitHub permissions."