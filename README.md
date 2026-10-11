# MiniBank — Mikroservis Tabanlı Para Transferi Platformu
 
Spring Boot öğrenme yolculuğumdaki **7. projem**. Önceki projelerde ([Student App](https://github.com/grkmuysl/spring-boot-student-crud-api), [Library Management](https://github.com/grkmuysl/library-management), [Multi User Todo](https://github.com/grkmuysl/spring-boot-multi-user-todo), [Order Service](https://github.com/grkmuysl/spring-boot-order-service-app), [Event Booking](https://github.com/grkmuysl/spring-boot-event-booking-application) ve [Birthday Tracker](https://github.com/grkmuysl/full-stack-birthday-tracker-app)) tek bir uygulama içinde CRUD, DTO, validasyon, hata yönetimi, JWT, migration, cache, rate limiting ve outbox gibi konuları çalışmıştım. Bu projede odak, özellikle **bankacılık sektöründe** önemli olan üç alana kaydı:
 
- **Kendi Dockerfile'ımı yazmak** ve tüm sistemi container'larla çalıştırmak
- **Güvenlik:** Kendi ürettiğim JWT yerine OAuth2 / OpenID Connect ve Keycloak
- **Mikroservisler:** Servisler arası iletişim, Kafka, Saga ve dağıtık sistemlerde veri tutarlılığı
Domain bilinçli olarak küçük tutuldu: müşteri hesapları ve hesaplar arası para transferi. Böylece emek iş kurallarından çok mimariye, güvenliğe ve tutarlılığa gitti.
 
Bu proje production için değil, **öğrenme amaçlı** geliştirilmiştir ve hâlâ geliştirme aşamasındadır.
 
## Yapay Zeka Kullanımı Hakkında
 
> **Bu projenin kodlarının neredeyse tamamı bir yapay zeka asistanı (Claude) ile yazılmıştır.**
 
Önceki projelerimde kodu kendim yazıyor, yapay zekadan en fazla mimari tartışmalarda yardım alıyordum. Bu projede yaklaşımı bilinçli olarak değiştirdim. Amaç, **yeni ve kapsamlı konuları** (Keycloak, Kafka ile Saga, outbox/inbox, dağıtık rate limiting, multi-stage Docker build vb.) daha hızlı öğrenmekti. Bu konuların her birini sıfırdan, deneme-yanılma ile yazmak çok daha uzun sürecekti.
 
Çalışma şeklim şuydu:
 
- Proje, her biri küçük ve kendi içinde çalışır durumda biten **adımlara** bölündü (1.1, 1.2 … 5.5).
- Her adımda önce **neden** öyle yapıldığını, hangi alternatiflerin olduğunu ve tuzakları tartıştık.
- Kod yapay zeka ile üretildi; ben her adımı çalıştırdım, curl ve SQL ile test ettim, hataları birlikte ayıkladık.
- Bir adım, "ne zaman bitmiş sayılır" kriterlerini sağlamadan bir sonrakine geçilmedi.
Bu yüzden bu projeyi önceki projeler gibi "kendi yazdığım kod" olarak değil, **"kavramları öğrenmek için yönlendirdiğim, çalıştırıp test ettiğim ve sonuçlarını anladığım bir proje"** olarak görüyorum. Buradaki konuları kalıcı hâle getirmek için bir sonraki projelerde kodu yeniden kendim yazmayı planlıyorum.
 
## Önceki Projelere Göre Neler Değişti / Neler Öğrenildi
 
### Docker
 
- **Kendi Dockerfile'ım:** Önce bilerek tek aşamalı bir Dockerfile yazıldı, image boyutu ve yeniden build süresi ölçüldü. Ardından multi-stage yapıya geçildi ve fark karşılaştırıldı.
- **Katman ve cache yönetimi:** `pom.xml` kaynak koddan önce kopyalanıyor, bağımlılıklar ayrı bir katmanda iniyor. Spring Boot'un katmanlı jar özelliğiyle (`-Djarmode=tools extract --layers`) kod değişikliğinde yalnızca küçük uygulama katmanı değişiyor.
- **Container güvenliği:** Çalışma aşamasında JDK yerine JRE, root olmayan kullanıcı, `.dockerignore`.
- **Compose:** Healthcheck'e bağlı `depends_on`, `profiles` ile altyapı ve uygulama servislerinin ayrılması, container ağında servis adıyla iletişim (`postgres:5432` ile `localhost:5433` arasındaki fark).
- **Multi-module Maven + Docker:** Build context'in proje kökü olması, `-pl` / `-am` ile tek modül build'i.
### Güvenlik
 
- **OAuth2 / OIDC + Keycloak:** Önceki projelerdeki `JwtService`, `JwtAuthenticationFilter` ve `JWT_SECRET` tamamen kaldırıldı. Token'ları Keycloak üretiyor; servisler yalnızca Resource Server olarak doğruluyor.
- **Asimetrik imza (RS256):** Servislerde hiçbir gizli anahtar yok; Keycloak'un açık anahtarları JWKS adresinden indiriliyor.
- **Token doğrulama:** İmza, `exp`, `iss` ve **`aud` (audience)** kontrolü. Her servis yalnızca kendisi için üretilmiş token'ı kabul ediyor.
- **Rol eşleme:** Keycloak'un `realm_access.roles` claim'i özel bir converter ile Spring `GrantedAuthority`'lerine çevriliyor; `@PreAuthorize` ile rol kontrolü yapılıyor (`CUSTOMER`, `OPERATOR`).
- **Kayıt bazlı sahiplik:** Hesaplar token'daki `sub` değerine bağlı. Başkasının kaydı için 403 yerine 404 dönülüyor (enumeration koruması).
- **Kimlik bilgisini token'dan almak:** Hesap sahibinin adı ve ID'si istemciden değil, doğrulanmış token'dan alınıyor.
- **Servisler arası kimlik:** Senkron akış aşamasında servisler birbirini kullanıcı token'ıyla değil, kendi kimlikleriyle (client credentials) çağırdı.
- **Zero trust:** Gateway token'ı erken doğruluyor ama servisler de doğrulamaya devam ediyor.
- **En az yetki:** Her servisin kendi veritabanı kullanıcısı var; bir servis başka bir servisin veritabanına bağlanamıyor.
- **Keycloak brute force koruması** ile login denemelerinin sınırlandırılması.
### Finansal doğruluk
 
- **Para ile doğru çalışmak:** `BigDecimal`, `NUMERIC(19,2)`, tutarlı ölçek (`setScale(2)`), `compareTo` ile karşılaştırma.
- **Veritabanı son savunma hattı:** `CHECK (balance >= 0)`, `CHECK (held_amount <= balance)` gibi kısıtlar.
- **IBAN üretimi ve doğrulaması:** TR IBAN formatı ve mod 97 kontrol hanesi algoritması.
- **Ledger (çift taraflı kayıt):** Her kesinleşen transfer için bir `DEBIT` ve bir `CREDIT` kaydı.
- **Bloke (provizyon) modeli:** Toplam bakiye ile kullanılabilir bakiye ayrımı; hold, capture ve release işlemleri.
- **Eşzamanlılık:** Tek hesap işlemlerinde optimistic lock (`@Version`), iki hesaplı işlemlerde **sıralı pessimistic lock** ile deadlock önleme.
- **Değişmez kontrolleri:** "Transferler para yaratmaz ve yok etmez" kuralını doğrulayan mutabakat sorguları.
### Mikroservisler ve dağıtık sistemler
 
- **Database-per-service:** Her servisin kendi veritabanı; servisler birbirlerinin tablolarını okumuyor.
- **Ortak kod modülü yok:** Servisler yalnızca HTTP ve mesaj sözleşmeleriyle birbirine bağlı ("dağıtık monolit" tuzağından kaçınmak için).
- **Uçtan uca idempotency:** İstemci tarafında `Idempotency-Key` header'ı, veritabanında `UNIQUE` kısıtları, iş seviyesinde `transferId` bazlı tekrar koruması.
- **API Gateway (Spring Cloud Gateway MVC):** Tek giriş noktası, `/internal` yollarının dışarıya kapalı olması, servis portlarının dışarıya açılmaması.
- **Correlation ID:** Gateway'de üretilen `X-Request-Id` tüm servislere ve Kafka mesajlarına taşınıyor, MDC ile her log satırına yazılıyor, hata cevaplarında referans numarası olarak dönüyor.
- **Dağıtık rate limiting:** Bucket4j + Redis ile IP, kullanıcı ve transfer bazlı limitler; birden fazla gateway instance'ı arasında ortak sayaç; Redis erişilemezse fail-open.
### Kafka ve Saga
 
- **Saga orkestrasyonu:** Transfer akışını transfer-service yönetiyor; adımlar geri alınabilirden geri alınamaza doğru sıralı (bloke → fraud kontrolü → kesinleştirme).
- **Telafi (compensation):** Fraud reddi, kesinleştirme hatası veya zaman aşımında bloke çözülüyor.
- **Durum makinesi:** İzin verilmeyen geçişler reddediliyor; geç veya tekrar gelen mesajlar durumu geri götüremiyor.
- **Saga zaman aşımı:** Fraud kontrolü belirli sürede sonuçlanmazsa bloke otomatik çözülüyor. Pivot adımından sonra geri dönüş yapılmıyor.
- **Outbox + Inbox:** Veritabanı değişikliği ile mesaj yayını aynı transaction'da; tekrar teslim edilen mesajlar `processed_messages` tablosuyla ayıklanıyor.
- **Partition key seçimi:** Komutlar için `transferId` (transfer bazlı sıralama), fraud istekleri için `initiatorId` (kullanıcı bazlı sıralı işleme, velocity kuralının doğru sayması için).
- **Hata sınıflandırması:** İş hataları cevap mesajına, geçici altyapı hataları sabırlı retry'a, zehirli mesajlar ve değişmez ihlalleri doğrudan DLT'ye.
- **Blocking retry tercihi:** Mesaj sırasını korumak için non-blocking retry topic'leri yerine blocking retry.
- **Chaos testleri:** Saga ortasında servisler, Kafka ve Postgres `docker kill` ile öldürülüp değişmez kontrolleri tekrar çalıştırıldı.
## Kullanılan Teknolojiler
 
- Java 25
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator)
- Spring Security — OAuth2 Resource Server
- Spring Cloud Gateway (Server MVC)
- Spring for Apache Kafka
- Keycloak 26
- PostgreSQL 17
- Apache Kafka (KRaft) + Kafka UI
- Redis
- Flyway
- Bucket4j + Caffeine
- Lombok
- JUnit 5 + Testcontainers
- Maven (multi-module)
- Docker / Docker Compose
## Mimari
 
### Servisler
 
```
minibank/
├── pom.xml              ← parent (packaging: pom)
├── compose.yaml
├── infra/postgres/init/ ← servis başına veritabanı ve kullanıcı scriptleri
├── api-gateway/         ← tek giriş noktası (8000)
├── account-service/     ← hesaplar, bakiye, bloke, ledger (8080)
├── transfer-service/    ← transferler, Saga orkestratörü (8081)
└── fraud-service/       ← dolandırıcılık kuralları (8082)
```
 
| Servis | Sorumluluğu | Veritabanı |
| --- | --- | --- |
| `api-gateway` | Yönlendirme, erken token doğrulama, correlation ID, rate limiting | — (Redis) |
| `account-service` | Hesap açma, para yatırma/çekme, dondurma, bloke/kesinleştirme/çözme, ledger | `accountdb` |
| `transfer-service` | Transfer talebi, idempotency, Saga durum makinesi, zaman aşımı | `transferdb` |
| `fraud-service` | Kural tabanlı dolandırıcılık kontrolü, karar kaydı (denetim izi) | `frauddb` |
| Keycloak | Kimlik doğrulama, token üretimi, roller | Kendi gömülü veritabanı |
 
### Trafik
 
```
                        ┌──► account-service  (/api/v1/accounts/**)
İstemci ──► Gateway ────┤
            (8000)      └──► transfer-service (/api/v1/transfers/**)
 
transfer-service ◄──── Kafka ────► account-service
       ▲                               
       └────────── Kafka ────────► fraud-service
```
 
- **Dışarıdan içeriye** gelen istekler yalnızca gateway üzerinden geçiyor.
- **Servisler arası** iletişim Kafka üzerinden, gateway'e uğramadan yapılıyor.
- Container modunda servislerin portları dışarıya kapalı; yalnızca gateway (8000) ve Keycloak (8180) açık.
### Transfer Saga'sı
 
```
                    ┌─ HOLD_FAILED ──────────────────────────────► FAILED
                    │
PENDING ──[bloke]──►┤
                    │                       ┌─ REJECTED ──[blokeyi çöz]──► REJECTED  (telafi)
                    └─ FUNDS_HELD ──[fraud]─┤
                                            └─ APPROVED ──[kesinleştir]──► COMPLETED
```
 
| Durum | Anlamı | Son durum |
| --- | --- | --- |
| `PENDING` | Bloke istendi | |
| `FUNDS_HELD` | Bloke konuldu, fraud kontrolü bekleniyor | |
| `CAPTURING` | Fraud onayladı, kesinleştirme istendi | |
| `RELEASING` | Telafi sürüyor (sebep: `FRAUD_REJECTED`, `CAPTURE_FAILED` veya `FRAUD_TIMEOUT`) | |
| `COMPLETED` | Para karşı hesaba geçti | ✔ |
| `FAILED` | Bloke konulamadı (ör. bakiye yetersiz) | ✔ |
| `REJECTED` | Bloke çözüldü, para müşteride | ✔ |
 
Müşteriye bu iç durumlar gösterilmiyor; ara durumların hepsi `PROCESSING` olarak dönüyor.
 
### Kafka Topic'leri
 
| Topic | Sahibi | Mesajlar | Key |
| --- | --- | --- | --- |
| `account.commands` | account-service | `HOLD_FUNDS`, `CAPTURE_FUNDS`, `RELEASE_FUNDS` | `transferId` |
| `account.replies` | account-service | `FUNDS_HELD`, `HOLD_FAILED`, `FUNDS_CAPTURED`, `CAPTURE_FAILED`, `FUNDS_RELEASED` | `transferId` |
| `fraud.check-requests` | fraud-service | `FRAUD_CHECK_REQUESTED` | `initiatorId` |
| `fraud.results` | fraud-service | `FRAUD_APPROVED`, `FRAUD_REJECTED` | `transferId` |
| `*.DLT` | Tüketen servis | İşlenemeyen mesajlar | Orijinal key |
 
Mesajlar `messageId`, `type`, `transferId`, `requestId`, `occurredAt` ve `payload` alanlarından oluşan bir zarf (envelope) içinde JSON olarak gönderiliyor. Servisler arasında ortak Java sınıfı paylaşılmıyor; sözleşme JSON biçimi.
 
### Fraud Kuralları
 
| Kural | Ne zaman tetiklenir | Kod |
| --- | --- | --- |
| `AmountLimitRule` | Tutar > 50.000 TRY | `AMOUNT_LIMIT_EXCEEDED` |
| `VelocityRule` | Son 10 dakikada 3 veya daha fazla transfer | `VELOCITY_LIMIT` |
| `BlockedBeneficiaryRule` | Alıcı IBAN yasaklı listede | `BLOCKED_BENEFICIARY` |
| `NewBeneficiaryHighAmountRule` | Daha önce onaylı transfer yapılmamış alıcıya > 10.000 TRY | `NEW_BENEFICIARY_HIGH_AMOUNT` |
 
Kurallar Strategy deseniyle ayrı sınıflar olarak yazıldı; eşik değerleri yapılandırmadan okunuyor. Her kararın gerekçesi `fraud_checks` tablosunda saklanıyor ve aynı transfer için karar bir kez verildikten sonra değişmiyor.
 
## Güvenlik Modeli
 
Keycloak'ta `minibank` adında bir realm kullanılıyor.
 
| Öğe | Açıklama |
| --- | --- |
| `CUSTOMER` rolü | Hesap açma, kendi hesaplarını görme, para çekme, transfer |
| `OPERATOR` rolü | Tüm hesapları görme, para yatırma, hesap dondurma/aktifleştirme |
| `minibank-test` client'ı | Yalnızca test için, curl ile token almaya yarayan public client (Direct access grants) |
| `account-service`, `transfer-service` client scope'ları | Token'a ilgili servisin audience değerini ekleyen mapper'lar |
 
**Yetki tablosu (account-service):**
 
| İşlem | CUSTOMER | OPERATOR |
| --- | --- | --- |
| Hesap açma | ✔ | ✘ |
| Hesap görüntüleme | ✔ (yalnızca kendi) | ✔ |
| Para yatırma | ✘ | ✔ |
| Para çekme | ✔ (yalnızca kendi) | ✔ |
| Dondurma / aktifleştirme | ✘ | ✔ |
 
> **Not:** Direct access grants (şifre ile doğrudan token alma) yalnızca curl ile test kolaylığı için açık. Gerçek bir istemcide Authorization Code + PKCE akışı kullanılmalı.
 
## API Endpoint'leri
 
Tüm istekler gateway üzerinden yapılır: `http://localhost:8000`. Tüm endpoint'ler `Authorization: Bearer <token>` gerektirir.
 
### Account — `/api/v1/accounts`
 
| Metod | Endpoint | Açıklama | Yetki |
| --- | --- | --- | --- |
| POST | `/` | Hesap açar; IBAN'ı sunucu üretir, sahip bilgisi token'dan alınır | `CUSTOMER` |
| GET | `/` | Giriş yapan kullanıcının hesaplarını listeler | `CUSTOMER` |
| GET | `/{id}` | Hesap detayı (bakiye, bloke, kullanılabilir bakiye) | `CUSTOMER` (kendi), `OPERATOR` |
| POST | `/{id}/deposit` | Para yatırır | `OPERATOR` |
| POST | `/{id}/withdraw` | Para çeker | `CUSTOMER` (kendi), `OPERATOR` |
| PATCH | `/{id}/freeze` | Hesabı dondurur | `OPERATOR` |
| PATCH | `/{id}/activate` | Dondurulmuş hesabı aktifleştirir | `OPERATOR` |
 
### Transfer — `/api/v1/transfers`
 
| Metod | Endpoint | Açıklama | Yetki |
| --- | --- | --- | --- |
| POST | `/` | Transfer başlatır, `Idempotency-Key` header'ı zorunlu, **202 Accepted** döner | `CUSTOMER` |
| GET | `/{id}` | Transferin durumunu getirir | `CUSTOMER` (kendi) |
| GET | `/` | Kendi transferlerini listeler | `CUSTOMER` |
 
### Hata Cevapları
 
Hatalar `ProblemDetail` formatında, makinenin okuyabileceği bir `code` ve destek için bir `requestId` ile döner:
 
```json
{
  "status": 422,
  "title": "Unprocessable Content",
  "detail": "Yetersiz bakiye",
  "code": "INSUFFICIENT_BALANCE",
  "requestId": "7f3a2c1e-..."
}
```
 
| HTTP | Örnek `code` |
| --- | --- |
| 400 | Validasyon hatası, bozuk JSON, eksik `Idempotency-Key` |
| 401 | Token yok, geçersiz veya süresi dolmuş (sebep `WWW-Authenticate` header'ında) |
| 403 | `ACCESS_DENIED` |
| 404 | `ACCOUNT_NOT_FOUND` (başkasının kaydı dahil) |
| 409 | Eşzamanlı güncelleme, veritabanı kısıtı ihlali |
| 422 | `INSUFFICIENT_BALANCE`, `ACCOUNT_NOT_ACTIVE`, `CURRENCY_MISMATCH`, `IDEMPOTENCY_KEY_REUSED` |
| 429 | `RATE_LIMITED` (`Retry-After` header'ı ile) |
 
## Örnek İstekler
 
```bash
# Keycloak'tan token al (yalnızca test client'ı ile)
TOKEN=$(curl -s -X POST http://localhost:8180/realms/minibank/protocol/openid-connect/token \
  -d grant_type=password -d client_id=minibank-test \
  -d username=ayse -d password=<şifre> | jq -r .access_token)
 
# Hesap aç
curl -i -X POST http://localhost:8000/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency": "TRY"}'
 
# Hesaplarımı listele
curl http://localhost:8000/api/v1/accounts -H "Authorization: Bearer $TOKEN"
 
# Transfer başlat
curl -i -X POST http://localhost:8000/api/v1/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"sourceIban":"TR...","targetIban":"TR...","amount":150.00,"currency":"TRY","description":"Kira"}'
 
# Transfer durumunu takip et
curl http://localhost:8000/api/v1/transfers/1 -H "Authorization: Bearer $TOKEN"
```
 
## Veritabanı
 
Her servisin kendi veritabanı ve kendi sıradan (superuser olmayan) kullanıcısı var. Veritabanları ve kullanıcılar `infra/postgres/init/` altındaki scriptlerle, Postgres ilk kez başlatıldığında oluşturuluyor. Şemalar her serviste Flyway migration'larıyla yönetiliyor (`ddl-auto=validate`).
 
| Servis | Veritabanı | Kullanıcı | Başlıca tablolar |
| --- | --- | --- | --- |
| account-service | `accountdb` | `account_app` | `accounts`, `holds`, `postings`, `outbox_events`, `processed_messages` |
| transfer-service | `transferdb` | `transfer_app` | `transfers`, `outbox_events`, `processed_messages` |
| fraud-service | `frauddb` | `fraud_app` | `fraud_checks`, `blocked_ibans`, `outbox_events`, `processed_messages` |
 
> **Not:** Init scriptleri yalnızca boş bir volume'da çalışır. Script değişirse `docker volume rm minibank_pgdata` ile volume'u sıfırlamak gerekir.
 
## Kurulum ve Çalıştırma
 
### Gereksinimler
 
- JDK 25
- Docker Desktop
- Maven Wrapper (proje ile birlikte gelir)
### Keycloak
 
Realm ayarları şu an Keycloak admin konsolunda (`http://localhost:8180`, `admin` / `admin`) elle yapılıyor ve `keycloak-data` volume'unda saklanıyor:
 
1. `minibank` realm'i
2. `CUSTOMER` ve `OPERATOR` realm rolleri
3. `minibank-test` public client'ı (yalnızca Direct access grants)
4. `account-service` ve `transfer-service` client scope'ları (Audience mapper ile), `minibank-test`'e Default olarak eklenmiş
5. Test kullanıcıları (`ayse`, `mehmet` → `CUSTOMER`; `operator1` → `OPERATOR`); e-posta, ad ve soyad alanları dolu olmalı
### Geliştirme modu (servisler IDE'den)
 
```bash
git clone https://github.com/grkmuysl/minibank.git
cd minibank
 
# Yalnızca altyapı: Postgres, Keycloak, Kafka, Kafka UI, Redis
docker compose up -d
```
 
Ardından servisleri IDE'den çalıştırın. Bu modda servislere doğrudan da erişilebilir.
 
### Her şey container'da
 
```bash
docker compose --profile app up --build
```
 
| Araç | Adres |
| --- | --- |
| API Gateway | `http://localhost:8000` |
| Keycloak | `http://localhost:8180` |
| Kafka UI | `http://localhost:8090` |
| PostgreSQL (yalnızca geliştirme) | `localhost:5433` |
 
## Dayanıklılık Testleri ve Değişmez Kontrolleri
 
Sistem aşağıdaki senaryolarla test edildi:
 
- Saga ortasında account-service, transfer-service, fraud-service, Kafka ve Postgres'in `docker kill` ile öldürülmesi
- Aynı mesajın (aynı veya farklı `messageId` ile) tekrar gönderilmesi
- Bozuk JSON ve bilinmeyen mesaj tipinin doğrudan DLT'ye gitmesi
- Fraud-service kapalıyken bloke zaman aşımı ve geç gelen onayın yok sayılması
- Bloke ile kesinleştirme arasında hedef hesabın dondurulması
- Paralel transferlerde deadlock olmaması ve velocity kuralının doğru sayması
Her senaryodan sonra çalıştırılan mutabakat sorguları:
 
```sql
-- accountdb: toplam bakiye değişmemeli, açık bloke kalmamalı
SELECT SUM(balance), SUM(held_amount) FROM accounts;
SELECT COUNT(*) FROM holds WHERE status = 'HELD';
 
-- accountdb: her transfer için borç ve alacak eşit olmalı
SELECT transfer_id
FROM postings
GROUP BY transfer_id
HAVING SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END) <> 0;
 
-- transferdb: ortada kalan transfer olmamalı
SELECT status, COUNT(*) FROM transfers GROUP BY status;
```
 
## Hâlâ Eksik Olanlar
 
Bu proje öğrenme amaçlıdır ve production-ready değildir. Şu an bilinçli olarak bırakılan ya da sonraki adımlarda ele alınacak konular:
 
- **Gözlemlenebilirlik (observability) yok:** Metrikler (Micrometer, Prometheus), panolar (Grafana) ve Kafka dahil uçtan uca tracing (OpenTelemetry) henüz eklenmedi. Takılan Saga'lar ve DLT mesajları şu an yalnızca log ile izleniyor, gerçek alarmlar yok.
- **Kafka güvenliği yok:** Kafka kimlik doğrulamasız çalışıyor; ağdaki herkes komut topic'lerine mesaj yazabilir. SASL kimlik doğrulaması ve topic bazlı ACL'ler eklenmeli.
- **Sır yönetimi geliştirme seviyesinde:** Şifreler `.env` ve yml'daki varsayılan değerlerde. Vault gibi bir secret manager kullanılmalı.
- **İleri güvenlik konuları yapılmadı:** Yüksek tutarlı transferlerde OTP ile ek doğrulama (step-up authentication), TCKN gibi hassas verilerin alan bazlı şifrelenmesi ve log maskeleme, servisler arası mTLS.
- **Gerçek bir istemci yok:** Authorization Code + PKCE akışını kullanan bir web veya mobil istemci yazılmadı.
- **Çoklu instance desteği eksik:** Outbox poller'ları ve zaman aşımı job'ları birden fazla instance'ta aynı kayıtları işleyebilir; ShedLock veya `SKIP LOCKED` ile koordinasyon gerekli.
- **Bloke süresi sınırsız:** Gerçek bankalardaki gibi belirli süre sonra otomatik çözülen bloke mekanizması yok.
- **CI/CD yok:** GitHub Actions ile otomatik build, test ve image güvenlik taraması (Trivy) eklenmedi.
## Lisans
 
Bu proje kişisel öğrenme amaçlı geliştirilmiştir.
 
