# Obol — specifikacija za implementaciju

Ovaj dokument je izvor istine za izgradnju aplikacije. Vizualni mockupi svih
ekrana su u `mockups/` — to su obične HTML stranice, otvori ih u pregledniku
ili pročitaj njihov markup za točne boje, veličine i razmake.

---

## 1. Što gradimo

Android aplikacija za praćenje pretplata. Korisnik ručno unosi svoje
pretplate, a aplikacija mu pokazuje koliko ga mjesečno koštaju, javlja prije
svake naplate i upozorava kad probni period ili promotivna cijena prelaze u
punu cijenu.

Sve radi lokalno na uređaju. Nema servera, nema korisničkih računa, nema
prijave, nema spajanja na banku ni e-mail.

- **Naziv:** Obol
- **Studio:** TM Studio
- **Package:** `com.tmstudio.obol`
- **minSdk:** 26 · **targetSdk:** najnoviji stabilni
- **Jezik:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Baza:** Room
- **Pozadinski poslovi:** WorkManager
- **Postavke:** DataStore (Preferences)
- **Jezik sučelja:** hrvatski; svi tekstovi idu u `strings.xml`, ništa
  hardkodirano u Composableima

---

## 2. Opseg v1

**Ulazi u v1:**

1. Dodavanje pretplate iz ugrađene baze servisa ili ručno
2. Postavljanje plana — cijena, ciklus, probni period, promotivna cijena
3. Početni pregled s ukupnim mjesečnim troškom i nadolazećim naplatama
4. Detalji pojedine pretplate, uređivanje i brisanje
5. Kalendar naplata po mjesecu
6. Podsjetnici (naplata, kraj probnog perioda, kraj promocije)
7. Mjesečna provjera korištenja
8. Postavke

**NE ulazi u v1** (mockupi postoje, dolaze kasnije):

- Ekran statistike (`mockups/Statistika.html`)
- Ekran ušteda i preporuka (`mockups/Ustede.html`)
- Pro kupnja, reklame, izvoz podataka
- Svijetla tema
- Sinkronizacija, widgeti

Donja navigacija u v1 ima tri taba: Pregled, Kalendar, Postavke. Mjesta za
Statistiku predviđeno je, ali tab se ne prikazuje dok ekran ne postoji.

---

## 3. Dizajn tokeni

Definiraj ih kao Compose `Color` konstante i provuci kroz `MaterialTheme`.
Nemoj ih pisati inline po Composableima.

### Boje (tamna tema, jedina u v1)

| Token | Hex | Gdje |
|---|---|---|
| `bg` | `#0C1014` | pozadina ekrana |
| `surface` | `#161C23` | kartice, polja, redovi liste |
| `surfaceAlt` | `#1E252E` | istaknute ćelije kalendara, segmenti |
| `navBg` | `#0F141A` | donja navigacija |
| `border` | `#1E252E` | obrub kartica |
| `borderStrong` | `#37424F` | obrub sekundarnih gumba |
| `divider` | `#1A212A` | crte u listama |
| `textPrimary` | `#F3F5F7` | glavni tekst |
| `textSecondary` | `#97A3B2` | podnaslovi, oznake |
| `textTertiary` | `#5E6B7A` | strelice, vrlo tihi tekst |
| `accent` | `#2FD39B` | mint — primarna akcija, brend |
| `onAccent` | `#06170F` | tekst na mint podlozi |
| `accentSurface` | `#11241C` | podloga mint kartica |
| `accentBorder` | `#1F4436` | obrub mint kartica |
| `accentText` | `#93C6B2` | sekundarni tekst u mint karticama |
| `warning` | `#FF8168` | koral — poskupljenje, brisanje |
| `warningSurface` | `#1C1316` | podloga koralnih kartica |
| `warningBorder` | `#3A2228` | obrub koralnih kartica |
| `trial` | `#FFC75A` | amber — probni period |
| `trialSurface` | `#1F1810` | podloga amber kartica |
| `trialBorder` | `#3D3118` | obrub amber kartica |
| `trialText` | `#E0C184` | sekundarni tekst u amber karticama |

Boje kategorija za točkice i monograme:
`#FF8168` (zabava), `#2FD39B` (glazba), `#7FB0FF` (gaming),
`#A99CF5` (streaming ostalo), `#E8C766` (pohrana), `#8FB8FF` (alati).

**Pravilo o značenju boja:** mint znači ušteda i potvrda, koral znači
poskupljenje i opasnost, amber znači probni period. Nikad ih ne koristi
dekorativno.

### Tipografija

Font: **Manrope** (Google Fonts), težine 400, 500, 600, 700, 800.
Dodaj ga kao `FontFamily` preko `androidx.compose.ui.text.googlefonts` ili
kao ugrađene `.ttf` datoteke u `res/font/`.

| Stil | Veličina | Težina | Napomena |
|---|---|---|---|
| hero (ukupni iznos) | 50sp | 800 | letterSpacing `-0.035em`, tabularne brojke |
| naslov ekrana | 22sp | 800 | letterSpacing `-0.02em` |
| naslov kartice | 15sp | 700 | |
| naslov sekcije | 14sp | 700 | |
| tijelo | 13–14sp | 500–600 | |
| oznaka / caption | 11–12sp | 600–700 | |
| velika oznaka | 11sp | 700 | VELIKA SLOVA, letterSpacing `0.1em` |

**Svi iznosi novca koriste tabularne brojke** (`FontFeature "tnum"`), inače
se stupci cijena u listama ne poravnavaju.

### Oblici i razmaci

- Kartice: 16–19dp · pločice ikona: 12–13dp · gumbi: 13–16dp · bedževi: 9–11dp
- Padding ekrana: 20dp sa strane
- Padding kartice: 14–16dp
- Razmak između sekcija: 18–24dp
- Razmak između redova liste: 10dp
- **Svaki element koji se tapka mora biti barem 44dp visok**

### Ikona aplikacije

Mint podloga `#2FD39B`, tamni prsten `#06231A`, prsten s malim prekidom
gore desno — istovremeno kovanica i slovo O. Izvor: `mockups/IkonaB.html`.
Adaptivna ikona: podloga puni mint, prednji sloj prsten.

---

## 4. Podatkovni model

Ovo je najvažniji dio. Ako model ne podnese pitanje *"koliko ova pretplata
košta na datum X"*, trial i promo logika se raspadne čim se krene računati
mjesečni zbroj.

### Pravila koja se ne krše

- **Novac je `Int` u centima.** Nikad `Double`, nikad `Float`.
  `1399` je 13,99 €.
- **Datumi su `java.time.LocalDate`.** Nikad `Date`, nikad `Calendar`.
  Room ih sprema kao `Long` (epochDay) preko TypeConvertera.
- Valuta je zasad uvijek `"EUR"`, ali polje postoji od prvog dana.

### Entiteti

**`Service`** — katalog servisa, učitan iz `assets/services.json`, read-only

```
id: String            // slug, npr. "netflix"
name: String          // "Netflix"
category: Category
colorHex: String      // boja monograma
monogram: String      // "N"
plans: List<CatalogPlan>   // name, priceCents, note
```

**`Subscription`** — korisnikova pretplata

```
id: Long (PK, autogenerate)
serviceId: String?        // null kad je ručno unesena
name: String
monogram: String
colorHex: String
category: Category
cycle: BillingCycle       // WEEKLY, MONTHLY, QUARTERLY, SEMIANNUAL, YEARLY, CUSTOM_DAYS
cycleDays: Int?           // samo za CUSTOM_DAYS
basePriceCents: Int       // puna cijena
currency: String          // "EUR"
firstBillingDate: LocalDate
planLabel: String?        // "Premium 4K"
notes: String?
isActive: Boolean
cancelledOn: LocalDate?
createdAt: Instant
updatedAt: Instant
```

**`TrialPeriod`** — najviše jedan po pretplati

```
subscriptionId: Long (FK, unique)
endsOn: LocalDate       // zadnji dan probnog perioda
priceCents: Int         // obično 0
```

**`PromoPeriod`** — najviše jedan po pretplati

```
subscriptionId: Long (FK, unique)
priceCents: Int
startsOn: LocalDate
endsOn: LocalDate       // izračunat iz broja ciklusa pri unosu i spremljen
```

Korisnik unosi broj ciklusa ("3 mjeseca"), a `endsOn` se izračuna i spremi.
Tako kasnije računanje ne mora ponovno simulirati cikluse.

**`PriceChange`** — povijest promjena cijene

```
id: Long (PK)
subscriptionId: Long (FK)
effectiveFrom: LocalDate
oldPriceCents: Int
newPriceCents: Int
recordedAt: Instant
```

**`UsageCheck`** — odgovor na mjesečno pitanje "koristiš li ovo"

```
subscriptionId: Long (FK)
period: YearMonth        // sprema se kao Int, npr. 202610
used: Boolean
answeredAt: Instant
PK: (subscriptionId, period)
```

### Ključne funkcije (domenski sloj, ne u DAO)

```kotlin
/**
 * Cijena koja vrijedi na zadani datum, u centima.
 * Redoslijed provjere je obavezan:
 *   1. probni period: date <= trial.endsOn      -> trial.priceCents
 *   2. promo:  promo.startsOn <= date <= promo.endsOn -> promo.priceCents
 *   3. inače:  basePriceCents, uz zadnji PriceChange čiji je
 *              effectiveFrom <= date
 */
fun Subscription.priceOn(date: LocalDate): Int

/** Sljedeći datum naplate na ili nakon zadanog datuma. */
fun Subscription.nextBillingOnOrAfter(date: LocalDate): LocalDate

/**
 * Mjesečni ekvivalent cijene na zadani datum.
 * YEARLY / 12, SEMIANNUAL / 6, QUARTERLY / 3, WEEKLY * 52 / 12,
 * CUSTOM_DAYS: priceOn(date) * 365 / cycleDays / 12
 * Zaokruživanje: HALF_UP na cijeli cent.
 */
fun Subscription.monthlyEquivalentOn(date: LocalDate): Int

/** Sve naplate u zadanom mjesecu, s iznosom koji vrijedi na taj dan. */
fun Subscription.billingsIn(month: YearMonth): List<BillingEvent>
```

`BillingEvent` je izračunat u letu, ne sprema se u bazu:
`data class BillingEvent(val subscriptionId: Long, val date: LocalDate, val priceCents: Int)`

**Ukupni mjesečni trošak** na početnom ekranu je zbroj
`monthlyEquivalentOn(today)` za sve aktivne pretplate. Dok traje probni
period ili promocija, u zbroj ulazi niža cijena — to je namjerno, aplikacija
mora pokazivati stvarno stanje, ne buduće.

**Rubni slučajevi koje treba pokriti testovima:**

- Naplata 31. u mjesecu, a mjesec ima 30 ili 28 dana → naplata ide zadnji dan
  tog mjeseca
- Prijestupna godina, 29. veljače
- Probni period koji završava isti dan kad je i naplata
- Promocija koja počinje istog dana kad završava probni period
- Pretplata dodana s datumom prve naplate u prošlosti

---

## 5. Podsjetnici

Jedan dnevni `CoroutineWorker` preko WorkManagera, zakazan za 09:00 po
lokalnom vremenu (vrijeme se može mijenjati u postavkama). Worker svaki dan
provjeri sve aktivne pretplate i pošalje obavijesti:

| Događaj | Kad | Primjer teksta |
|---|---|---|
| Nadolazeća naplata | N dana prije (default 3) | „Netflix ti se naplaćuje za 3 dana — 13,99 €" |
| Kraj probnog perioda | 2 dana prije | „Disney+ ti za 2 dana prestaje probni period. Prva naplata je 9,99 €." |
| Kraj promocije | 3 dana prije | „Spotify ti od 1. prosinca ide s 5,99 na 10,99 €." |
| Provjera korištenja | 1. u mjesecu | „Jesi li prošli mjesec koristio Netflix?" |

Tri kanala obavijesti: `naplate`, `probni_periodi`, `provjere`.

Traži `POST_NOTIFICATIONS` dozvolu na Androidu 13+, i to tek kad korisnik
doda prvu pretplatu, ne na prvom pokretanju.

`BOOT_COMPLETED` receiver koji ponovno zakaže worker nakon restarta uređaja.

---

## 6. Ekrani

Mockup svakog ekrana je u `mockups/`. Čitaj ih za točne vrijednosti.

| # | Mockup | Ekran | Sadržaj |
|---|---|---|---|
| 1 | `Main.html` | Pregled | Logo i ime gore, ukupni mjesečni trošak, kartica ušteda (u v1 vodi na prazno stanje ili se skriva), nadolazeće naplate, lista svih pretplata |
| 2 | `Dodaj.html` | Dodaj pretplatu | Pretraga, mreža popularnih servisa, „dodaj ručno" |
| 3 | `Plan.html` | Postavi plan | Servis, cijena, ciklus, prekidač za probni period, prekidač za promo cijenu, dan naplate |
| 4 | `Detalji.html` | Detalji pretplate | Cijena, sljedeća naplata, pitanje o korištenju, povijest naplate, uredi i obriši |
| 5 | `Kalendar.html` | Kalendar | Mjesečna mreža s točkicama u boji servisa, popis naplata tog mjeseca |
| 6 | `Postavke.html` | Postavke | Podsjetnici, valuta, tema, izvoz (u v1 bez Pro kartice) |
| — | `Statistika.html` | *(v1.1)* | Ne gradi u v1 |
| — | `Ustede.html` | *(v1.1)* | Ne gradi u v1 |

**Navigacija:** Compose Navigation, tri taba u donjoj traci (Pregled,
Kalendar, Postavke). Ekrani 2, 3 i 4 su odredišta s povratnom strelicom, bez
donje trake.

**Tok dodavanja:** Pregled → Dodaj pretplatu → Postavi plan → spremljeno,
natrag na Pregled.

**Prazna stanja:** kad nema nijedne pretplate, Pregled pokazuje kratku
poruku i veliki gumb za dodavanje, ne prazan zbroj od 0,00 €.

---

## 7. Baza servisa

`app/src/main/assets/services.json`, oko 100 servisa s cijenama za Hrvatsku.
Učitava se pri prvom pokretanju u Room, pa se dalje čita iz baze.

```json
{
  "version": 1,
  "updatedOn": "2026-10-01",
  "services": [
    {
      "id": "netflix",
      "name": "Netflix",
      "category": "ENTERTAINMENT",
      "monogram": "N",
      "colorHex": "#FF8168",
      "plans": [
        { "name": "Standard s reklamama", "priceCents": 599 },
        { "name": "Standard", "priceCents": 1099 },
        { "name": "Premium", "priceCents": 1399, "note": "4K + HDR" }
      ]
    }
  ]
}
```

Kategorije: `ENTERTAINMENT`, `MUSIC`, `GAMING`, `STORAGE`, `TOOLS`,
`EDUCATION`, `FITNESS`, `NEWS`, `OTHER`.

U prvom koraku napravi datoteku s 15–20 servisa koje ljudi u Hrvatskoj
stvarno koriste (Netflix, Spotify, HBO Max, Disney+, YouTube Premium,
PlayStation Plus, Xbox Game Pass, Nintendo Switch Online, iCloud+, Google
One, Microsoft 365, Duolingo, Strava, Canva, Adobe). Ostalo se dopunjava
kasnije — ne blokiraj razvoj na unosu podataka.

---

## 8. Formatiranje

- Novac: `13,99 €` — zarez kao decimalni razdjelnik, razmak prije znaka.
  Koristi `NumberFormat.getCurrencyInstance(Locale("hr", "HR"))`.
- Datum dugi: `4. listopada` · datum kratki: `4. 10. 2026.`
- Relativno vrijeme: `za 3 dana`, `sutra`, `danas`
- Mjeseci se pišu malim slovom: *listopada*, ne *Listopada*

---

## 9. Privatnost

Aplikacija nema `INTERNET` dozvolu u v1. Nema analitike, nema crash
reportinga koji šalje podatke van, nema reklamnih SDK-ova.

To nije samo tehnička odluka nego prodajni argument — stavlja se u opis na
Play Storeu i u prazna stanja aplikacije.

Jedina dozvola je `POST_NOTIFICATIONS`.

---

## 10. Čega se kloniti

- Ne spajaj se na banke, e-mail, SMS ni notifikacije drugih aplikacija.
  Google to odbija, a korisnici s pravom ne vjeruju.
- Ne dodavaj prijavu ni korisnički račun.
- Ne gradi ekrane statistike i ušteda u v1.
- Ne koristi emoji kao ikone. Material ikone ili vlastiti vektori.
- Ne piši boje i veličine inline po Composableima — sve kroz temu.
- Ne koristi `Double` za novac.

---

## 11. Redoslijed izgradnje

1. Projekt, ovisnosti, tema s bojama i fontom, prazna navigacija s tri taba
2. Room entiteti, DAO-i, TypeConverteri, migracije
3. Domenske funkcije `priceOn`, `nextBillingOnOrAfter`,
   `monthlyEquivalentOn` — **s unit testovima prije UI-ja**
4. `services.json` i učitavanje u bazu
5. Ekran Pregled s pravim podacima
6. Tok Dodaj → Postavi plan
7. Detalji, uređivanje, brisanje
8. Kalendar
9. WorkManager i obavijesti
10. Postavke
11. Prazna stanja, ikona, naziv aplikacije

Treća točka je ona koja odlučuje hoće li projekt raditi. Napravi je kako
treba i s testovima prije nego dotakneš ijedan ekran.
