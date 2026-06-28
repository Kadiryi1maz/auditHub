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

