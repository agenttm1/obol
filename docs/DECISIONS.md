# Obol — odluke i odstupanja od specifikacije

Dopuna uz `OBOL_SPEC.md`. Spec ostaje izvor istine; ovdje su odluke donesene
tijekom izgradnje v1 tamo gdje spec šuti, proturječi sam sebi ili ga je
trebalo dopuniti. Svaka stavka ima razlog, da se kasnije može svjesno promijeniti.

Stanje: svih 11 koraka iz poglavlja 11 je napravljeno.

## Podatkovni model

- **`CatalogPlan` ima polje `cycle`** (zadano `MONTHLY`). Bez njega bi godišnji
  plan iz kataloga ušao u pretplatu kao mjesečni (npr. Microsoft 365 za 99 € kao
  99 € mjesečno). Sprema se u JSON stupac, pa shema baze nije promijenjena.
- **Domenske funkcije rade nad `SubscriptionWithDetails`**, a ne nad `Subscription`,
  jer samo ta klasa ima probni period, promociju i povijest cijena.
- **Povijest cijena ima prednost pred `basePriceCents`** u `priceOn`: prije prve
  promjene vrijedi njezina `oldPriceCents`, od nje `newPriceCents`. Tako najavljeno
  poskupljenje radi bez obzira na to je li `basePriceCents` već postavljen na novu cijenu.
- **`Subscription.serviceId` nije strani ključ** na katalog: osvježavanje kataloga
  ne smije dirati korisnikove pretplate. Ime, monogram i boja se kopiraju pri unosu.
- **Logotipi servisa** umjesto monograma, gdje postoje (`ServiceLogos`): 11 servisa
  dobiva jednobojni znak iz Simple Icons 16.34.0, obojen bojom servisa na istoj
  prigušenoj podlozi kao monogram. Disney+, Xbox, Nintendo, Microsoft 365, Canva i
  Adobe imaju monogram — ti su znakovi uklonjeni iz Simple Icons na zahtjev vlasnika,
  pa se ne uzimaju ni iz starijih verzija. Google One koristi Googleovo „G" jer
  nema vlastiti znak. Logotipi su ugrađeni u aplikaciju (bez interneta) i ne
  smiju se koristiti u ikoni aplikacije ni kao glavni motiv materijala za Play.
- **Kategorije bez boje u paleti** (`EDUCATION`, `FITNESS`, `NEWS`, `OTHER`) koriste
  boju alata `#8FB8FF`.

## Unos i uređivanje

- **Prva naplata je dan nakon kraja probnog perioda.** Pravilo iz spec-a
  (`date <= trial.endsOn` → cijena probnog perioda) bi inače naplatu na zadnji
  dan probnog perioda učinilo besplatnom. Forma zato sama postavlja dan naplate.
- **Promocija počinje prvom plaćenom naplatom** i traje 1–24 ciklusa; `endsOn` je
  dan prije prve pune naplate. Promo cijena mora biti niža od pune.
- **Promjena pune cijene pri uređivanju** upisuje `PriceChange`; zadani datum
  „Nova cijena vrijedi od" je sljedeća naplata.
- **Popularni servisi** na ekranu Dodaj su popis u kodu (`POPULAR_SERVICE_IDS`),
  ne polje u `services.json`.
- Ispunjena forma se ne čuva ako Android ugasi proces u pozadini.

## Ekrani

- **Pregled:** kartica ušteda je skrivena (spec dopušta). „Sljedeće naplate" su
  tri najbliže. Iznos u listi je cijena po ciklusu (uz „/ god." i sl.); pretplata
  u probnom periodu pokazuje 0,00 € i oznaku. Promjena cijene se ističe 30 dana.
- **Detalji:** bez kartice „Preporuka" (vodi na Uštede, izvan v1) i bez gumba „⋮".
  „Godišnje" je po punoj cijeni. Pitanje o korištenju glasi „koristio", ne
  „gledao" (mora pristajati i uz Spotify i iCloud). Kad se iznos sljedeće naplate
  razlikuje od današnjeg, prikazuje se ispod datuma (koral = skuplje).
- **Kalendar:** zbroj je zbroj stvarnih naplata u mjesecu, ne mjesečnih
  ekvivalenata. Današnji dan ima neutralan obrub (koral znači poskupljenje).
  Kraj probnog perioda se ne prikazuje jer nije naplata.
- **Postavke:** bez Pro kartice i bez izvoza u CSV — poglavlje 2 izvoz izričito
  stavlja izvan v1, iako ga tablica u poglavlju 6 spominje. Dodan je redak
  „Vrijeme podsjetnika" (spec traži da se može mijenjati) i upozorenje kad su
  obavijesti isključene. Valuta i tema su prikazane, ali fiksne.
- Boje iz mockupa koje nisu u tablici tokena (npr. `#263040`, `#C8D1DB`) nisu
  uvedene; korišten je najbliži token. Podloge monograma su aproksimacija
  prozirnošću (`ObolColors.tintedBackground`).

## Podsjetnici

- **Lanac jednokratnih poslova** umjesto periodičnog od 24 h: svaki prolaz zakaže
  sljedeći za točno vrijeme iz postavki (periodični posao odluta i ne prati
  ljetno računanje vremena). Pokretanje aplikacije lanac obnavlja ako je pukao.
- Isti dan se ne šalje dvaput (zadnji prolaz se pamti u DataStoreu).
- Kad kraj probnog perioda ili promocije javlja istu naplatu, obični podsjetnik o
  naplati se ne šalje.
- Kraj promocije ide na kanal `probni_periodi` („Probni periodi i promocije").
- „Upozori na poskupljenje" gasi samo obavijest o kraju promocije; kraj probnog
  perioda je uvijek uključen.
- `BootReceiver` reagira i na promjenu vremena i vremenske zone.

## Platforma i privatnost

- **compileSdk i targetSdk 37** — trenutne AndroidX biblioteke ne rade s 36.
  Alati: Kotlin 2.4.20, AGP 9.4.1, Gradle 9.8.0, KSP 2.3.12.
- **Jezik sučelja je izričito hr-HR** (`AppLocale.kt`). Tekstovi su u zadanom
  `values/`, ali Android pravila množine uzima iz jezika uređaja — na engleskom
  telefonu bilo bi „3 mjeseci". Isto vrijedi za tekstove obavijesti.
- **Dozvole:** korisnik vidi samo `POST_NOTIFICATIONS`. WorkManager uz to treba
  sistemske `WAKE_LOCK` i `RECEIVE_BOOT_COMPLETED`. `ACCESS_NETWORK_STATE` i
  `FOREGROUND_SERVICE`, koje WorkManager dodaje, uklonjene su. `INTERNET` nema.
- **Sigurnosna kopija:** ništa ne ide u oblak (aplikacija obećava da podaci
  ostaju na uređaju). Izravni prijenos na novi telefon prenosi bazu i postavke
  (Android 9+).
- **Release build** koristi R8 (smanjivanje koda i resursa) bez dodatnih pravila;
  provjereno na emulatoru: katalog, navigacija, Room i WorkManager rade.

## Za kasnije (izvan v1)

- Cijene po državi korisnika i „live" cijene. Live cijene traže `INTERNET` i ruše
  obećanje iz poglavlja 9.
- Logotipi za servise koji ih zasad nemaju — samo uz dopuštenje vlasnika znaka.
- Statistika, Uštede, Pro, izvoz podataka (spec, poglavlje 2).
- Otkazivanje pretplate (`isActive` / `cancelledOn` postoje u bazi, UI nema).

## Održavanje kataloga

`app/src/main/assets/services.json`: nakon svake izmjene podigni `version` —
aplikacija tada pri sljedećem pokretanju osvježi bazu. `CatalogFileTest` provjerava
ispravnost datoteke (jedinstveni id-jevi, boje, cijene, servisi iz spec-a).

Cijene u verziji 3 (4. 10. 2026.): ručno provjerene za Hrvatsku; Xbox Game Pass
prema cjeniku Microsofta u eurima od 21. 4. 2026. (Essential 8,99, Premium 12,99,
Ultimate 20,99, PC Game Pass 12,99).

## Testovi

```
./gradlew testDebugUnitTest            # domena, formatiranje, katalog, forme
./gradlew connectedDebugAndroidTest    # Room, obavijesti, WorkManager (treba uređaj)
```

## Licence

Manrope (`app/src/main/res/font/manrope.ttf`) — SIL Open Font License 1.1,
tekst u `docs/licenses/Manrope-OFL.txt`.

Logotipi servisa (`app/src/main/res/drawable/logo_*.xml`) — Simple Icons 16.34.0,
CC0 1.0 (https://simpleicons.org). Sami znakovi su zaštićeni žigovi svojih
vlasnika; koriste se samo da bi korisnik prepoznao servis koji plaća.

## Objava (release)

Release se potpisuje ključem iz `keystore.properties` (predložak:
`keystore.properties.example`). Ni ključ ni ta datoteka ne idu u git. Ključ se
napravi jednom i čuva izvan repozitorija — bez njega se aplikacija na Playu ne
može ažurirati:

```
keytool -genkeypair -v -keystore ../obol-upload.jks -alias obol-upload -keyalg RSA -keysize 4096 -validity 10000
```

Zatim `./gradlew bundleRelease` daje `app/build/outputs/bundle/release/app-release.aab`
za Play Console (uz uključen Play App Signing ovo je ključ za upload).
