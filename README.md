# AuditHub - Static Distribution

Bu repo artık ASP.NET Core uygulaması içerir; fakat takımınız için sunucu gerektirmeyen bir *statik* görüntüleyici hazırladım. Bu statik versiyon, `data/dashboard.json` dosyasını okuyup tarayıcıda gösterir — sunucu tarafı bağımlılığı yoktur.

## 📦 Kurulum ve Çalıştırma

1. Repository'yi klonlayın veya proje dosyalarını indirin.

```bash
git clone https://bitbucket.thy.com/scm/mycq/audithub.git
cd audithub
```

2. `index.html` dosyasını modern bir tarayıcıda (Chrome, Edge vb.) açın.

- Eğer dosyaları zip olarak paylaşacaksanız, kullanıcılar `index.html`'i açıp uygulamayı görebilir.
- `data/dashboard.json` dosyasının `index.html` ile aynı kök dizinde (`./data/dashboard.json`) olduğundan emin olun.

3. Kütüphaneler kullanmayan basit bir istemci renderer kullanıyoruz; aktif bir internet bağlantısı gerekli değildir.

## Yapı

- `index.html` - Statik istemci uygulaması
- `data/dashboard.json` - Görüntülenen veri
- `wwwroot/` - CSS/JS varlıkları (isteğe bağlı)

## Docker / Sunucu

Bu statik dağıtım sunucu gerektirmez. Eğer merkezi olarak host etmek isterseniz, `Dockerfile` kullanarak bir container oluşturup herkesin erişebileceği bir sunucuya (ör. Azure App Service, AKS, Docker host) deploy edebilirsiniz.

## Notlar

- Statik viewer veri kaynaklarını `./data/dashboard.json` üzerinden alır. Sunucu tarafı işlem (Jira çağrıları vb.) gereken işlevsellikler desteklenmez.

## 🚀 Paylaşma ve Otomatik Deploy

Aşağıda hem **GitHub Pages** hem de **Azure Static Website** kullanarak nasıl yayınlayacağınız ve ayrıca `audithub-static.zip` dosyasını Bitbucket Downloads üzerinden nasıl paylaşacağınız gösterilmiştir.

### GitHub Pages

1. GitHub'da yeni bir repository oluşturun (ör. `audithub`).
2. Mevcut repository'yi GitHub'a itmek için (local çalışma dizininde):

```powershell
# GitHub repo URL'sini girin (ör: https://github.com/yourorg/audithub.git)
$githubUrl = Read-Host 'GitHub repository URL'
git remote add github $githubUrl
git push github master
```

3. GitHub Actions workflow zaten eklendi (`.github/workflows/gh-pages.yml`) — push yaptıktan sonra site otomatik olarak GitHub Pages'e deploy edilecektir.

4. GitHub Pages URL'sine erişin: `https://<your-username>.github.io/<repo>` (repository ayarlarından Pages URL'sini kontrol edin).

### Azure Static Website

1. Azure CLI kurulu ve `az login` ile giriş yapılmış olmalı.
2. Script'i çalıştırın (parametreleri gerektiği gibi değiştirin):

```powershell
# Örnek kullanım
.\azure-deploy.ps1 -ResourceGroupName myResourceGroup -StorageAccountName mystaticstore -Location westeurope
```

3. Script, `audithub-static.zip` paketini kullanarak Azure Storage Static Website'ine dosyaları yükleyecektir; script sonunda size site URL'sini yazacaktır.

### Bitbucket Downloads

1. Bitbucket repository sayfasına gidin.
2. `Downloads` sekmesine gidip `audithub-static.zip` dosyasını yükleyin.
3. Takım arkadaşlarınız dosyayı indirip açtıktan sonra `index.html`'i tarayıcıda çalıştırabilirler.

> Not: `audithub-static.zip` zaten repoda oluşturuldu. Eğer farklı bir sürüm oluşturmak isterseniz, yerel dizinde şu komutla yeni zip üretebilirsiniz:

```powershell
# Proje kökünden
Compress-Archive -Path index.html, data, wwwroot -DestinationPath audithub-static.zip -Force
```

### Yardımcı Script: GitHub'a Push

Bir GitHub repo URL'si verdiğinizde remote ekleyip push yapan `push-to-github.ps1` helper script'i eklendi. Kullanımı:

```powershell
# Çalıştırın ve GitHub repo URL'sini girin
.\push-to-github.ps1
```

İsterseniz ben GitHub'da repo açıp dosyaları pushlayabilirim (GitHub erişim bilgisi veya personal access token gerekir). Azure deploy'u da sizin adınıza çalıştırabilmem için Azure aboneliği ve gerekli izinler gerekecektir.

