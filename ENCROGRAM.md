# Encrogram

Verschlüsselte Nachrichten über Telegram – als Werkzeug im FoldBack-Launcher.

Encrogram verschlüsselt Text mit einem **Sicherheitssatz**, den beide Gesprächspartner vorher **mündlich** ausgetauscht haben.
Über Telegram läuft nur unlesbarer Geheimtext der Form `TENCDEC:1:…`. Weder Telegram noch jemand, der den Chat mitliest,
kann den Inhalt ohne den Sicherheitssatz lesen.

> **Status:** Erste Version umgesetzt (Branch `encrogram`). Unit-Tests laufen; der Test auf einem echten Gerät steht noch aus
> (siehe [Offene Punkte](#12-offene-punkte)).

---

## Inhalt

1. [Getroffene Entscheidungen](#1-getroffene-entscheidungen)
2. [Workflow](#2-workflow)
3. [Architektur und Ordnerstruktur](#3-architektur-und-ordnerstruktur)
4. [Kryptografie](#4-kryptografie)
5. [Der Sicherheitssatz](#5-der-sicherheitssatz)
6. [Sitzung und Sperre](#6-sitzung-und-sperre)
7. [Schutz auf dem Gerät](#7-schutz-auf-dem-gerät)
8. [Bedrohungsmodell](#8-bedrohungsmodell)
9. [Icon](#9-icon)
10. [Einbindung in FoldBack](#10-einbindung-in-foldback)
11. [Tests](#11-tests)
12. [Offene Punkte](#12-offene-punkte)

---

## 1. Getroffene Entscheidungen

| Thema | Entscheidung | Begründung |
|---|---|---|
| Name | **Encrogram** | Verbindet *Telegram* und *EncroChat*. |
| Anbindung an Telegram | **Variante B: Teilen und Textmenü**, kein Telegram-Login | Encrogram bekommt keinen Zugriff auf den Telegram-Account und keine Datenbank mit Chats. Funktioniert auch mit jedem anderen Messenger. |
| Nicht gewählt: Variante A (TDLib) | — | Eine eigene Telegram-Sitzung in der App wäre ein großes Risiko (voller Account-Zugriff), braucht `api_id`/`api_hash`, macht die APK ~20–30 MB größer und legt eine lokale Chat-Datenbank an. |
| Nachrichten-Präfix | `TENCDEC:` | Aus der ursprünglichen Idee übernommen. Dahinter folgt eine Versionsnummer, damit sich das Verfahren später ändern lässt. |
| Verschlüsselung | **Argon2id + AES-256-GCM** | Siehe [Kapitel 4](#4-kryptografie). |
| Länge des Sicherheitssatzes | **8 zufällige Wörter** (≈ 104 Bit) | Siehe [Kapitel 5](#5-der-sicherheitssatz). |
| Wortliste | Deutsches Wörterbuch des Projektinhabers, beim Laden gefiltert (8209 Wörter) | Wird nur zum *Erzeugen* gebraucht, nicht zum Ver- oder Entschlüsseln. |
| Speicherung des Satzes | **Nie** – weder auf dem Speicher noch als Hash | Ein gespeicherter Hash würde Offline-Raten auf einem gestohlenen Handy ermöglichen. |
| Löschen aus dem RAM | **Sofort beim Ausschalten des Bildschirms** und **nach 2 Minuten ohne Aktivität** (zusätzlich manuell über „Sperren“) | Liegt das Handy herum, ist nichts mehr entschlüsselbar. |
| Eigener Satz mit weniger als 8 Wörtern | **Erlaubt, aber mit deutlicher Warnung** | Selbst gewählte Sätze sind schwächer als zufällige. |
| Erinnerung zum Satzwechsel | **Hinweis nach 30 Tagen** | Begrenzt den Schaden, falls ein Satz unbemerkt bekannt wurde. |
| Absender | **Name und Geräte-Kennung, beide verschlüsselt** in jeder Nachricht | Gespiegelte eigene Nachrichten fallen auf. Siehe [4.5](#45-absender-und-sendezeit). |
| Code-Struktur | Objektorientiert, eigenes Paket `encrogram/`, eine Klasse pro Verantwortung | Testbar, austauschbar, vom Launcher getrennt. |
| Icon | Schwarz-weißes Strich-Icon: Papierflieger und kantiges „E“ | Passt zu den Outlined-Icons des Launchers. Siehe [Kapitel 9](#9-icon). |

---

## 2. Workflow

### 2.1 Einmalig: Namen festlegen

Beim ersten Öffnen fragt Encrogram nach deinem Namen. Er geht **verschlüsselt** mit jeder Nachricht mit,
damit dein Gegenüber sieht, von wem sie ist. Höchstens 32 Byte (etwa 32 Zeichen ohne Umlaute).
Ändern lässt er sich jederzeit über „Dein Name: … ändern“.

### 2.2 Neuen Sicherheitssatz vereinbaren (persönlich)

1. FoldBack → Menü → Werkzeuge → **Encrogram** → „Neuen Satz erzeugen“.
2. Encrogram zeigt 8 zufällige Wörter, z. B.
   `ofen wolke spitz ruder kamel neun birke tal`
3. Ihr gebt den Satz **mündlich und unter vier Augen** weiter. Nicht über Telegram, nicht per SMS, nicht fotografieren.
4. „Diesen Satz verwenden“ bzw. beim Gegenüber den Satz eintippen und „Entsperren“.
5. Encrogram zeigt zwei **Prüfwörter**, z. B. `TIGER · LAMPE`. Stimmen sie bei beiden überein, habt ihr denselben Satz.

### 2.3 Nachricht senden

1. Encrogram öffnen. Ist kein Satz entsperrt, fragt die App nach dem Sicherheitssatz.
2. Satz über seine Prüfwörter auswählen (nur nötig, wenn mehrere entsperrt sind).
3. Nachricht ins Textfeld schreiben. Ein Zähler zeigt, wie viel noch hineinpasst.
4. „Verschlüsselt senden“ tippen.
5. Encrogram verschlüsselt den Text und übergibt den Geheimtext per `ACTION_SEND` an Telegram.
6. In Telegram den Chat auswählen. Der Geheimtext steht im Eingabefeld, dann auf Senden tippen.
7. Das Textfeld in Encrogram ist danach leer.

Bei Telegram kommt **nur Geheimtext** an. Der Klartext steht nie in einem Eingabefeld von Telegram, also auch nie in einem Telegram-Entwurf.

### 2.4 Nachricht empfangen und lesen

**Normaler Weg (Textmenü):**

1. In Telegram den Text einer `TENCDEC:`-Nachricht markieren.
2. Im Textmenü **„Entschlüsseln“** wählen. Diesen Eintrag meldet Encrogram per `ACTION_PROCESS_TEXT` an.
3. Ein Overlay von Encrogram öffnet sich über Telegram:
   - Ist ein passender Satz entsperrt, erscheint der Klartext sofort.
   - Sonst fragt Encrogram zuerst nach dem Sicherheitssatz.
4. Overlay schließen oder verlassen: Der Klartext ist weg und wurde nirgends gespeichert.

Der markierte Text wird direkt von Android an Encrogram übergeben. Er geht **nicht über die Zwischenablage**.
Rutscht beim Markieren etwas Text davor oder danach mit hinein, ist das egal – Encrogram sucht sich die Nachricht heraus.

**Ausweichweg (Zwischenablage):**

Telegram nutzt eine eigene Textauswahl. Ob dort fremde `PROCESS_TEXT`-Einträge erscheinen, muss auf dem Gerät geprüft werden
(Test: Google Übersetzen installieren und in Telegram Text markieren). Erscheint „Entschlüsseln“ nicht:

1. In Telegram die Nachricht kopieren.
2. Encrogram → „Aus Zwischenablage entschlüsseln“.
3. Encrogram liest die Zwischenablage und **leert sie sofort**.

In der Zwischenablage liegt dabei nur Geheimtext, nie Klartext.

### 2.5 Mehrere Gesprächspartner

Jedes Paar hat seinen eigenen Satz. Encrogram kennt keine Chats und speichert keine Namen von Gesprächspartnern.

- In einer Sitzung können **mehrere Sätze gleichzeitig entsperrt** sein („+ Weiteren Satz entsperren“).
- Beim **Entschlüsseln** probiert Encrogram alle entsperrten Sätze durch.
- Beim **Senden** wählst du den Satz über seine **Prüfwörter** aus, ohne dass der Satz selbst sichtbar ist.

### 2.6 Mögliche Anzeigen

| Situation | Anzeige |
|---|---|
| Entschlüsselung erfolgreich | „Von: Name · Datum, Uhrzeit“, Prüfwörter des passenden Satzes, Klartext |
| Kein Satz entsperrt | „Gib den Sicherheitssatz ein …“ und das Eingabefeld |
| Kein entsperrter Satz passt | „Mit den entsperrten Sätzen nicht lesbar“ und das Eingabefeld |
| Text ist keine `TENCDEC`-Nachricht | „Keine Encrogram-Nachricht …“ |
| Nachricht beschädigt oder abgeschnitten | „Die Nachricht ist beschädigt oder unvollständig.“ |
| Unbekannte Version, z. B. `TENCDEC:2:` | „Mit einer neueren Encrogram-Version erstellt.“ |
| Nachricht stammt vom eigenen Gerät | ⚠ „Diese Nachricht wurde auf diesem Gerät verschlüsselt …“ |
| Nachricht trägt den eigenen Namen | ⚠ „Diese Nachricht trägt deinen Namen …“ |

---

## 3. Architektur und Ordnerstruktur

```
app/src/main/
├── java/com/example/foldback/
│   ├── LauncherApps.kt                  Eintrag „Encrogram“ unter Werkzeuge
│   │
│   └── encrogram/
│       ├── Encrogram.kt                 Einstiegspunkt (einmal pro Prozess): verbindet alle Teile,
│       │                                registriert die Sperren, DecryptOutcome, SenderWarning
│       ├── crypto/
│       │   ├── KeyDeriver.kt            Sicherheitssatz + Salt → 32-Byte-Schlüssel (Argon2id), Argon2Params
│       │   ├── SecretKey.kt             Hält den Schlüssel im RAM; wipe() überschreibt mit Nullen
│       │   ├── AesGcm.kt                AES-256-GCM: verschlüsseln und authentifizieren
│       │   ├── Encryptor.kt             Klartext → TencdecMessage; berechnet die verbleibende Länge
│       │   ├── Decryptor.kt             TencdecMessage → DecryptResult (Erfolg, falscher Schlüssel, beschädigt)
│       │   └── Padding.kt               Auffüllen auf 64-Byte-Blöcke und wieder entfernen
│       │
│       ├── format/
│       │   ├── TencdecMessage.kt        Salt, Nonce, Geheimtext; parse() und toText(); ParseResult
│       │   ├── InnerPayload.kt          Zeitstempel, Geräte-Kennung, Name und Text; Sender
│       │   └── Base64Url.kt             Base64url ohne Padding
│       │
│       ├── passphrase/
│       │   ├── Passphrase.kt            Normalisierter Satz als ByteArray; wipe()
│       │   ├── PassphraseNormalizer.kt  NFC, Kleinschreibung, Leerzeichen zusammenfassen, Wörter zählen
│       │   ├── Wordlist.kt              Lädt, filtert und prüft res/raw/german_words.txt
│       │   ├── PassphraseGenerator.kt   8 Wörter mit SecureRandom
│       │   └── VerificationWords.kt     Zwei Prüfwörter zum mündlichen Abgleich
│       │
│       ├── session/
│       │   ├── EncrogramSession.kt      Entsperrte Sätze im RAM; lockAll()
│       │   ├── UnlockedPhrase.kt        Ein entsperrter Satz mit seinen Schlüsseln je Salt
│       │   ├── ScreenOffLocker.kt       Bildschirm aus → sperren
│       │   ├── InactivityLocker.kt      2 Minuten ohne Aktivität → sperren
│       │   ├── EncrogramSettings.kt     Gespeichert: Geräte-Kennung, eigener Name, Datum des letzten Satzes
│       │   └── RotationReminder.kt      Hinweis nach 30 Tagen ohne neuen Satz
│       │
│       ├── transfer/
│       │   ├── MessengerSender.kt       ACTION_SEND an Telegram, sonst Auswahl-Dialog
│       │   └── ClipboardReader.kt       Ausweichweg: lesen und sofort leeren
│       │
│       └── ui/
│           ├── EncrogramActivity.kt     Hauptseite, eigene Activity mit FLAG_SECURE
│           ├── EncrogramViewModel.kt    Zustand der Hauptseite
│           ├── EncrogramScreen.kt       Entsperren, Generator, Schreiben und Senden
│           ├── DecryptActivity.kt       Overlay für „Entschlüsseln“ im Textmenü (PROCESS_TEXT)
│           ├── Components.kt            Schaltflächen, sicheres Textfeld, Satzeingabe, Ergebnisanzeige
│           └── EncrogramIcon.kt         Strich-Icon (ImageVector)
│
└── res/
    ├── raw/german_words.txt             Deutsches Wörterbuch (vom Projektinhaber bereitgestellt)
    ├── values/themes.xml                Theme.Encrogram.Dialog für das Overlay
    └── xml/backup_rules.xml, data_extraction_rules.xml   schließen encrogram.xml vom Backup aus

app/src/test/java/com/example/foldback/encrogram/
    ├── TestFixtures.kt                  Gemeinsame Bausteine, billige Argon2-Parameter
    ├── EncryptDecryptTest.kt
    ├── WrongPassphraseTest.kt
    ├── TamperingTest.kt
    ├── TencdecMessageTest.kt
    ├── FormatTest.kt                    Base64url, Padding, InnerPayload, Normalisierung
    ├── WordlistTest.kt                  Wortliste, Generator, Prüfwörter
    └── KeyDeriverTest.kt                Echte Argon2-Parameter
```

### 3.1 Wichtige Klassen

```kotlin
class KeyDeriver(params: Argon2Params = Argon2Params.DEFAULT) {
    fun derive(passphrase: Passphrase, salt: ByteArray): SecretKey
}

class Encryptor(random: SecureRandom, clock: () -> Long) {
    fun encrypt(text: String, sender: Sender, key: SecretKey, salt: ByteArray): TencdecMessage
    companion object { fun remainingBytes(text: String, senderName: String): Int }
}

class Decryptor {
    fun decrypt(message: TencdecMessage, key: SecretKey): DecryptResult   // Success | WrongKey | Malformed
}

class Passphrase private constructor(...) : AutoCloseable {
    companion object { fun fromInput(input: CharSequence): Passphrase }   // normalisiert sofort
    val wordCount: Int
    val isWeak: Boolean
    fun sameAs(other: Passphrase): Boolean                               // konstante Laufzeit
    fun wipe()
}

class Wordlist(words: Collection<String>) {                              // prüft Größe und Wörter
    companion object { fun parse(lines: Sequence<String>): Wordlist; fun load(context: Context): Wordlist }
}

class EncrogramSession(verify: (Passphrase) -> String, random: SecureRandom) {
    val phrases: StateFlow<List<UnlockedPhrase>>
    fun unlock(passphrase: Passphrase): UnlockedPhrase?
    fun lockAll()
}

class Encrogram {                                                         // Encrogram.get(context)
    suspend fun unlock(input: CharSequence): UnlockedPhrase?
    suspend fun encrypt(text: String, phrase: UnlockedPhrase): String?
    suspend fun decrypt(text: CharSequence): DecryptOutcome
    suspend fun generatePhrase(): List<String>
    fun touch()                                                           // Aktivität melden
    fun lock()
}
```

**Grundsätze:**

- **Eine Klasse, eine Aufgabe.** `Encryptor` weiß nichts über Telegram, `MessengerSender` nichts über Kryptografie.
- **Abhängigkeiten werden übergeben, nicht selbst erzeugt.** `Encryptor` bekommt Zufall und Uhr von außen, `KeyDeriver` seine Parameter. So sind Tests deterministisch und schnell.
- **Geheime Daten sind gekapselt.** `Passphrase` und `SecretKey` geben ihr rohes `ByteArray` nur an Klassen im Modul heraus, implementieren `AutoCloseable` und haben ein `toString()` von `***`.
- **Fehler sind Typen, keine Exceptions.** `DecryptResult` und `DecryptOutcome` machen einen falschen Satz zu einem normalen Ergebnis, das die UI freundlich anzeigt.
- **Rechenintensives läuft nicht im UI-Thread.** Argon2id läuft über `Encrogram` auf `Dispatchers.Default`.

---

## 4. Kryptografie

### 4.1 Verfahren

| Baustein | Wahl | Parameter |
|---|---|---|
| Schlüsselableitung | **Argon2id** (Version 1.3), Bouncy Castle | 64 MiB Speicher, 3 Durchläufe, 1 Thread, 32 Byte Ausgabe |
| Verschlüsselung und Integrität | **AES-256-GCM** (AEAD), Java-Kryptografie von Android | 256-Bit-Schlüssel, 96-Bit-Nonce, 128-Bit-Tag |
| Zufall | `SecureRandom` | — |

**Warum diese Wahl:**

- **Argon2id** ist der aktuelle Standard für passwortbasierte Schlüssel (Sieger der Password Hashing Competition, RFC 9106).
  Der hohe Speicherbedarf macht Rateversuche auf Grafikkarten und Spezialhardware teuer.
- **AES-256-GCM** ist auf jedem Android-Gerät eingebaut, meist hardwarebeschleunigt, und verschlüsselt und authentifiziert in
  einem Schritt: Jede Veränderung am Geheimtext wird erkannt.
- **Bouncy Castle** (reines Java) liefert Argon2id, das Android selbst nicht mitbringt. Es braucht keine nativen Bibliotheken
  und läuft deshalb auch in den JVM-Unit-Tests.

**Abweichung vom ersten Konzept:** Geplant waren XChaCha20-Poly1305 und libsodium (`lazysodium-android`). Das hätte native
Bibliotheken und JNA gebraucht, die in JVM-Unit-Tests nicht laufen. XChaCha20 wurde gewählt, weil es zufällige Nonces ohne
Kollisionsgefahr erlaubt. Bei Encrogram verschlüsselt aber jeder Schlüssel nur die Nachrichten einer einzigen Sitzung
(siehe [4.4](#44-umgang-mit-dem-salt)). Bei so wenigen Nachrichten pro Schlüssel sind zufällige 96-Bit-Nonces von AES-GCM
genauso sicher (die Grenze liegt bei rund 2³² Nachrichten pro Schlüssel).

**Geschwindigkeit:** Eine Ableitung dauert auf einem PC etwa 0,3 Sekunden. Auf dem Handy wird mit 1–3 Sekunden gerechnet;
das muss auf dem Gerät noch gemessen werden.

### 4.2 Nachrichtenformat

```
TENCDEC:1:<base64url ohne Padding( salt[16] ‖ nonce[12] ‖ ciphertext ‖ tag[16] )>
```

| Teil | Größe | Inhalt |
|---|---|---|
| `TENCDEC:` | 8 Zeichen | Erkennungszeichen |
| `1:` | 2 Zeichen | Formatversion |
| `salt` | 16 Byte | Zufällig, einmal pro Sitzung und Satz (siehe 4.4) |
| `nonce` | 12 Byte | Zufällig für **jede** Nachricht |
| `ciphertext` | n × 64 Byte | Verschlüsselte, aufgefüllte innere Nutzlast |
| `tag` | 16 Byte | GCM-Authentifizierung |

**Associated Data (AAD):** die ASCII-Bytes `TENCDEC:1`, gefolgt vom Salt. Damit sind Version und Salt kryptografisch an den
Geheimtext gebunden. Ein veränderter Salt fällt auch dann auf, wenn der Schlüssel schon im Speicher bereitliegt.

Base64url wird verwendet, weil es keine Zeichen wie `+` oder `/` enthält, die Messenger als Formatierung oder Link deuten könnten.
Encrogram bringt dafür eine eigene kleine Umsetzung mit, weil `java.util.Base64` erst ab API 26 existiert.

### 4.3 Innere Nutzlast (vor der Verschlüsselung)

```
sentAt[8] ‖ deviceTag[8] ‖ nameLength[1] ‖ name ‖ text (UTF-8) ‖ 0x80 ‖ 0x00 …
```

| Teil | Größe | Zweck |
|---|---|---|
| `sentAt` | 8 Byte, Unix-Sekunden | Sendezeit anzeigen; alte, erneut zugestellte Nachrichten fallen auf |
| `deviceTag` | 8 Byte | Erkennen, ob die Nachricht vom eigenen Gerät stammt |
| `nameLength`, `name` | 1 + höchstens 32 Byte | Absendername, für Menschen lesbar |
| `text` | variabel | Die eigentliche Nachricht |
| Padding | bis zum nächsten Vielfachen von 64 Byte | `0x80`, danach Nullbytes (ISO/IEC 7816-4). Verschleiert die genaue Länge. |

**Längenrechnung:** Eine Telegram-Nachricht hat höchstens 4096 Zeichen. Nach Abzug von 10 Zeichen Präfix, 44 Byte für Salt,
Nonce und Tag und der Base64-Umrechnung bleiben **3008 Byte** aufgefüllte Nutzlast. Davon gehen 17 Byte Kopf, der Name und
mindestens 1 Byte Padding ab. Mit einem Namen wie „Fabian“ passen also **2984 Byte Text** hinein. Umlaute brauchen 2 Byte,
Emojis 4. `Encryptor.MAX_PADDED_SIZE` wird aus diesen Größen berechnet, nicht fest eingetragen. Der Zähler unter dem
Nachrichtenfeld rechnet in Byte.

### 4.4 Umgang mit dem Salt

Argon2id ist absichtlich langsam. Ein neuer Salt pro Nachricht würde jedes Senden und jedes Lesen verzögern.

- **Senden:** Beim Entsperren eines Satzes erzeugt Encrogram einen zufälligen Sende-Salt. Beim ersten Senden wird der Schlüssel
  einmal abgeleitet. Alle Nachrichten dieser Sitzung nutzen diesen Salt und je eine neue zufällige Nonce.
- **Empfangen:** `UnlockedPhrase` hält abgeleitete Schlüssel im RAM, getrennt nach Salt. Mehrere Nachrichten aus derselben
  Sitzung des Gegenübers kosten also nur eine Ableitung.
- Nach dem Sperren ist alles weg. Die nächste Sitzung beginnt mit einem neuen Salt.

### 4.5 Absender und Sendezeit

Bei Variante B kennt Encrogram weder Chat noch Absender. Telegram (oder jemand mit Zugriff auf den Chat) könnte also:

- **Spiegeln:** eine eigene Nachricht so zurückschicken, als käme sie vom Gegenüber.
- **Wiedereinspielen:** eine alte Nachricht später erneut zustellen.

Ein Name **vor** dem Geheimtext würde nicht helfen: Jeder könnte ihn ändern, auch Telegram selbst. Deshalb steckt alles im
verschlüsselten, also fälschungssicheren inneren Teil:

- **Name** (für Menschen): wird beim Lesen als „Von: …“ angezeigt. Trägt eine Nachricht den eigenen Namen, warnt Encrogram.
  Funktioniert auch, wenn eine Person Encrogram auf mehreren Geräten nutzt.
- **Geräte-Kennung** (technisch, unsichtbar): 8 zufällige Byte, beim ersten Gebrauch erzeugt und in den privaten App-Daten
  gespeichert. Fängt den Fall ab, dass beide Seiten zufällig denselben Namen gewählt haben.
- **Sendezeit**: wird beim Lesen immer angezeigt. Eine alte, erneut zugestellte Nachricht fällt dadurch auf.

Grenzen: Den Namen wählt jeder selbst. Wer den Satz kennt, könnte sich als jemand anderes ausgeben. Bei zwei Personen ist das
unerheblich, denn außer euch kennt niemand den Satz.

### 4.6 Prüfwörter

```
v = Argon2id(satz, salt = "ENCROGRAM-VERIFY", gleiche Parameter)
Prüfwort 1 = wortliste[ (v[0] << 8 | v[1]) mod Listengröße ]
Prüfwort 2 = wortliste[ (v[2] << 8 | v[3]) mod Listengröße ]
```

- Die Prüfwörter hängen nur vom Satz ab. Bei gleichem Satz erhalten beide Geräte dieselben Wörter.
- Sie werden **nur angezeigt und nie gespeichert oder gesendet**. Gespeichert wären sie ein Prüfwert für Offline-Raten.
- Sie verraten etwa 26 Bit über den Satz, aber nur wer sie sieht. Bei 104 Bit bleibt das unkritisch. Trotzdem nicht in fremder Gegenwart offen zeigen.
- Beide Geräte brauchen dieselbe Wortliste, also dieselbe Encrogram-Version. Sonst unterscheiden sich die Prüfwörter, obwohl der Satz stimmt.

### 4.7 Normalisierung des Satzes

Vor jeder Verwendung wird der Satz so vereinheitlicht:

1. Unicode-NFC
2. Kleinschreibung mit `Locale.ROOT`
3. Leerzeichen am Anfang und Ende entfernen
4. Mehrere Leerzeichen, Tabs und Zeilenumbrüche zu einem Leerzeichen zusammenfassen

`Ofen  Wolke` und `ofen wolke` ergeben damit denselben Schlüssel.

---

## 5. Der Sicherheitssatz

### 5.1 Stärke von 8 Wörtern

Jedes Wort wird zufällig aus 8209 Wörtern gewählt: log₂(8209) ≈ 13,0 Bit pro Wort.

| Wörter | Entropie | Rateversuche im Mittel |
|---|---|---|
| 6 | 78 Bit | 1,5 · 10²³ |
| **8** | **104 Bit** | **1,0 · 10³¹** |
| 10 | 130 Bit | 6,9 · 10³⁸ |

**Zur Einordnung von 8 Wörtern:** Ohne jede Bremse schafft ein Angreifer mit 10¹² Versuchen pro Sekunde (ein großes Rechenzentrum)
etwa 10¹⁹ Sekunden. Das sind **rund 300 Milliarden Jahre**, mehr als das Zwanzigfache des Alters des Universums.
Argon2id mit 64 MiB macht jeden Versuch zusätzlich um viele Größenordnungen teurer.

**Ehrliche Einordnung:** Das anerkannte Maß für „langfristig physikalisch unknackbar“ ist 128 Bit. 8 Wörter liegen mit 104 Bit
darunter, erreichen mit Argon2id aber ein Sicherheitsniveau weit jenseits jedes realistischen Angreifers. 10 Wörter würden
128 Bit allein aus dem Zufall erreichen. Die Wortzahl ist eine einzige Konstante (`Passphrase.RECOMMENDED_WORDS`).

**Die Rechnung gilt nur für zufällig erzeugte Sätze.** Ein selbst ausgedachter Satz aus 8 Wörtern hat viel weniger Entropie,
weil Menschen vorhersagbar wählen. Deshalb:

- Der Generator ist der empfohlene Weg.
- Bei einem eigenen Satz mit weniger als 8 Wörtern zeigt Encrogram eine deutliche Warnung. Entsperrte schwache Sätze sind in der Liste als „schwach“ markiert.

### 5.2 Erzeugung

- `SecureRandom.nextInt(size)` wählt für jedes Wort einen gleichverteilten Index, ohne Modulo-Verzerrung.
- Echte Würfel sind nicht nötig. Der Zufallsgenerator von Android ist kryptografisch sicher.
- Der erzeugte Satz wird nur angezeigt. Es gibt **keinen „Kopieren“-Knopf**, damit er nicht in der Zwischenablage landet.
- Verlässt man Encrogram, während der Generator offen ist, wird der Satz verworfen.

### 5.3 Die Wortliste

Quelle ist `app/src/main/res/raw/german_words.txt`, ein allgemeines deutsches Wörterbuch mit 14 517 Einträgen
(© Uwe Schindler, Björn Jacke, LGPL 3.0; der Lizenzkopf bleibt in der Datei).
`Wordlist.parse` filtert daraus beim Laden die geeigneten Wörter:

| Regel | Grund |
|---|---|
| Kommentarzeilen (`#`) und leere Zeilen überspringen | Lizenzkopf |
| Nur **a–z**, keine Umlaute, kein ß | Keine Probleme mit ä/ae und mit unterschiedlichen Tastaturen |
| **4–9 Buchstaben** | Gut auszusprechen und zu tippen |
| **Fugen-s-Fragmente entfernen** (`abfahrts` neben `abfahrt`) | Sind keine eigenen Wörter und klingen fast wie das Grundwort |
| Doppelte entfernen | Doppelte Wörter senken die Entropie |
| Optional Diceware-Format `11111 wort` | Andere Listen funktionieren ebenfalls |
| Ergebnis muss **mindestens 7776** Wörter haben | Mindestens 12,9 Bit pro Wort wie bei klassischem Diceware |

Ergebnis: **8209 Wörter**. Ist die Liste zu klein oder ungültig, bricht `Wordlist` mit einer klaren Meldung ab;
`WordlistTest` stellt sicher, dass die mitgelieferte Liste passt.

**Bewusst nicht übernommen:** „Kein Wort ist der Anfang eines anderen“. Diese Regel braucht man nur, wenn Wörter ohne
Leerzeichen aneinandergehängt werden. Encrogram trennt Wörter immer mit Leerzeichen; die Regel hätte die Liste unter 7776 gedrückt.

Die Wortliste wird **nur zum Erzeugen** und für die Prüfwörter gebraucht. Zum Ver- und Entschlüsseln ist der Satz einfach Text.

### 5.4 Wechsel des Satzes

Es gibt **keine Forward Secrecy**. Wird ein Satz bekannt, sind alle damit verschlüsselten Nachrichten lesbar, die noch bei Telegram liegen.

Empfehlung:

- Satz **bei persönlichen Treffen** wechseln, wenn es passt.
- **Sofort** wechseln bei jedem Verdacht.
- Nach einem Wechsel alte Nachrichten in Telegram löschen.
- Nach dem Wechsel sind alte Nachrichten nur mit dem alten Satz lesbar. Wird er vergessen, sind sie endgültig unlesbar. Das ist gewollt.

**Erinnerung nach 30 Tagen:** `RotationReminder` speichert nur das **Datum**, an dem zuletzt ein Satz im Generator erzeugt und
übernommen wurde. Sind seitdem 30 Tage vergangen, zeigt Encrogram „Seit 30 Tagen kein neuer Satz. Beim nächsten Treffen wechseln?“.
Das Datum verrät nichts über den Satz. Einschränkung: Encrogram weiß nicht, welcher Satz zu welchem Gesprächspartner gehört,
und speichert dazu auch nichts. Die Erinnerung bezieht sich deshalb auf den zuletzt erzeugten Satz insgesamt.
Wer nur eigene Sätze eintippt und nie den Generator nutzt, bekommt keine Erinnerung.

---

## 6. Sitzung und Sperre

### 6.1 Was im RAM liegt

`EncrogramSession` lebt im Launcher-Prozess und hält **ausschließlich im Arbeitsspeicher**:

- die entsperrten, normalisierten Sätze (`Passphrase`, als `ByteArray`)
- die daraus abgeleiteten Schlüssel je Salt (`SecretKey`)
- pro Satz den eigenen Sende-Salt der aktuellen Sitzung

Nichts davon wird jemals gespeichert.

### 6.2 Wann gelöscht wird

| Auslöser | Umsetzung |
|---|---|
| **Bildschirm wird ausgeschaltet** | `ScreenOffLocker` empfängt `Intent.ACTION_SCREEN_OFF`. Er wird beim ersten Zugriff auf `Encrogram` am Application-Context registriert (dynamisch; über das Manifest kommt dieser Broadcast nicht an) und lebt so lange wie der Prozess. |
| **2 Minuten ohne Aktivität** | `InactivityLocker` startet bei jeder Aktivität einen Timer neu. Als Aktivität zählt jede Berührung und Eingabe in Encrogram und im Overlay (`onUserInteraction`) sowie jedes Entsperren, Verschlüsseln und Entschlüsseln. Zeit in Telegram zählt **nicht**. |
| Knopf „Sperren“ | `Encrogram.lock()` |
| Prozess wird beendet | RAM ist automatisch weg |

`lockAll()` ruft auf jedem Satz und Schlüssel `wipe()` auf und leert die Liste. Daraufhin löscht die Hauptseite Nachricht,
Ergebnis und erzeugten Satz und zeigt wieder die Satzeingabe.

**Wettlaufsituationen sind abgesichert:**

- Eine Verschlüsselung läuft unter derselben Sperre wie `wipe()`. Eine Sperre kann also nie mitten im Verschlüsseln einen halb
  gelöschten Schlüssel hinterlassen, der dann für eine Nachricht verwendet würde.
- Läuft gerade ein Entsperren (Argon2id), während gesperrt wird, wird das Ergebnis verworfen. Der Satz bleibt also nicht entsperrt.

### 6.3 Zusammenspiel mit der Anforderung „Satz bei jedem Aufruf“

Bei Variante B gibt es keine Chats in Encrogram. Die Regel lautet deshalb: **Nach jedem Einschalten des Bildschirms und nach
2 Minuten Pause muss der Satz neu eingegeben werden.** Wer zügig mehrere Nachrichten hintereinander liest oder schreibt,
muss den Satz nicht jedes Mal eintippen.

---

## 7. Schutz auf dem Gerät

| Risiko | Maßnahme |
|---|---|
| **Tastatur lernt mit** (Gboard-Wörterbuch, Cloud-Sync) | Alle Encrogram-Eingabefelder sind `SecureTextField`: ein `EditText` mit `IME_FLAG_NO_PERSONALIZED_LEARNING` und `TYPE_TEXT_FLAG_NO_SUGGESTIONS`. Compose bietet dieses Flag nicht direkt an. Das Satzfeld ist zusätzlich ein sichtbares Passwortfeld; dort schalten Tastaturen Lernen grundsätzlich ab. |
| **Drittanbieter-Tastatur** | Kann technisch alles mitlesen. Dagegen hilft keine App – nur eine vertrauenswürdige Tastatur. |
| **Passwortmanager bietet „Speichern?“ an** | `importantForAutofill = NO_EXCLUDE_DESCENDANTS` auf beiden Encrogram-Fenstern und `NO` auf jedem Feld. |
| **Screenshots, Bildschirmaufnahme, App-Übersicht** | `FLAG_SECURE` auf `EncrogramActivity` und `DecryptActivity`; beide mit `excludeFromRecents`. |
| **Zustand landet im Bundle** | Kein `rememberSaveable` für Satz oder Klartext; `EditText` mit `isSaveEnabled = false`. Zustand liegt nur im ViewModel bzw. in `remember`. |
| **Halb getippter Satz bleibt stehen** | Verlässt man die Hauptseite, wird das Satzfeld zurückgesetzt. Das Overlay schließt sich beim Verlassen ganz. |
| **Strings bleiben im RAM** | Schlüssel und Satz werden als `ByteArray` gehalten und mit `fill(0)` gelöscht. Eingabefelder und die Normalisierung nutzen intern Strings, die sich nicht löschen lassen. Das Auslesen des RAMs erfordert Root bzw. Gerätezugriff im entsperrten Zustand; dieses Restrisiko wird bewusst akzeptiert. |
| **Zwischenablage** | Nie automatisch kopieren. Kein Kopieren-Knopf für Satz oder Klartext; der entschlüsselte Text ist nicht markierbar. Der Ausweichweg leert die Zwischenablage sofort nach dem Lesen. |
| **Klartext landet in Telegram** | `DecryptActivity` gibt bewusst kein Ergebnis per `setResult` zurück. Sonst könnte der Klartext den markierten Text in Telegram ersetzen. |
| **Backups** | FoldBack hat `android:allowBackup="true"`. Encrogram speichert nur `encrogram.xml` (Geräte-Kennung, eigener Name, Datum); die Datei ist in `backup_rules.xml` und `data_extraction_rules.xml` ausgeschlossen, damit ein wiederhergestelltes Gerät eine eigene Kennung bekommt. |
| **Logs und Absturzberichte** | Kein `Log` mit Satz, Schlüssel oder Klartext. `toString()` von `Passphrase` und `SecretKey` gibt `***` zurück. |
| **Benachrichtigungen** | Telegram zeigt nur `TENCDEC:1:…`. Encrogram selbst erzeugt keine Benachrichtigungen. |

---

## 8. Bedrohungsmodell

### Encrogram schützt gegen

- Telegram selbst und jeden, der Zugriff auf die Telegram-Server oder den Telegram-Account hat
- Mitlesen von Chats, Backups oder Exporten aus Telegram
- Veränderung von Nachrichten unterwegs (wird durch GCM erkannt)
- Gespiegelte eigene Nachrichten (Name und Geräte-Kennung im verschlüsselten Teil)
- Offline-Raten des Satzes (8 Zufallswörter und Argon2id)
- Jemanden, der das Handy nach dem Ausschalten des Bildschirms oder nach 2 Minuten Pause in die Hand bekommt

### Encrogram schützt nicht gegen

- **Metadaten:** Telegram sieht, wer wann wie oft mit wem schreibt und dass Encrogram benutzt wird. Die Länge ist nur grob in 64-Byte-Stufen sichtbar.
- **Kompromittiertes Gerät:** Schadsoftware, Root-Zugriff, manipulierte Tastatur
- **Mithören beim mündlichen Austausch** oder einen aufgeschriebenen Satz
- **Bekanntwerden des Satzes:** Dann sind alle damit verschlüsselten Nachrichten lesbar, auch alte (keine Forward Secrecy)
- **Den Gesprächspartner selbst:** Wer den Satz kennt, kann Nachrichten lesen und schreiben und einen beliebigen Namen eintragen.
- **Wiedereinspielen ohne Hinsehen:** Eine alte Nachricht wird erneut korrekt entschlüsselt; nur die angezeigte Sendezeit verrät sie.
- **Jemanden, der innerhalb der 2 Minuten das entsperrte Handy nimmt**

### Mögliche spätere Erweiterung

Ein passwortgestützter Schlüsselaustausch (PAKE, z. B. CPace) würde Offline-Raten komplett ausschließen und Forward Secrecy ermöglichen.
Dafür müssten beide Seiten einmal gleichzeitig online sein. Das ist deutlich aufwendiger und deshalb nicht Teil der ersten Version.

---

## 9. Icon

**Idee:** Telegram und EncroChat verschmelzen, im Schwarz-Weiß-Stil des Launchers.

- **Grundform:** das kantige, abgeschrägte „E“ aus dem EncroChat-Logo, nach rechts offen wie eine eckige Klammer mit schrägen Ecken.
- **Mittelstrich:** Statt des mittleren Querbalkens fliegt der **Telegram-Papierflieger** aus der Klammer heraus nach rechts oben.
- **Stil:** Strich-Icon wie die übrigen Outlined-Icons und `GhostIcon`: 24 × 24 dp, Strichstärke 1,7, runde Enden, keine Füllung.
  Die Farbe kommt wie bei allen Launcher-Icons über `tint`.
- **Umsetzung:** `encrogram/ui/EncrogramIcon.kt` als `ImageVector`.
- **Hinweis:** Das Icon ist von beiden Logos *inspiriert* und kopiert keines davon. Für die private Nutzung im eigenen Launcher ist das unproblematisch.

```
   ┌────────╲
   │         ◁──╮      ← Papierflieger ersetzt den Mittelbalken
   │      ╱─────╯
   │
   └────────╱
```

---

## 10. Einbindung in FoldBack

### 10.1 Launcher-Eintrag

In `LauncherApps.kt` unter `toolsFolder`:

```kotlin
LauncherApp("Encrogram", EncrogramIcon) {
    Intent(it, EncrogramActivity::class.java)
},
```

Encrogram läuft in einer **eigenen Activity** und nicht als Compose-Unterseite des Launchers. Nur so lassen sich `FLAG_SECURE`
und das Autofill-Verbot gezielt für Encrogram setzen, ohne den Home-Bildschirm zu beeinflussen.
Drückt man die Home-Taste, räumt der Launcher (`singleTask`, `clearTaskOnLaunch`) Encrogram vom Stapel.

### 10.2 Manifest

```xml
<activity
    android:name=".encrogram.ui.EncrogramActivity"
    android:excludeFromRecents="true"
    android:exported="false"
    android:windowSoftInputMode="adjustResize" />

<activity
    android:name=".encrogram.ui.DecryptActivity"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"
    android:excludeFromRecents="true"
    android:exported="true"
    android:label="Entschlüsseln"
    android:noHistory="true"
    android:theme="@style/Theme.Encrogram.Dialog"
    android:windowSoftInputMode="adjustResize">
    <intent-filter>
        <action android:name="android.intent.action.PROCESS_TEXT" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
</activity>
```

- `DecryptActivity` muss `exported="true"` sein, damit andere Apps sie über das Textmenü aufrufen können. Sie verarbeitet nur
  Text und gibt nichts zurück.
- `configChanges` verhindert, dass ein Drehen des Handys das Overlay neu startet und damit schließt.
- `MessengerSender` probiert `org.telegram.messenger`, `org.telegram.messenger.web` und `org.thunderdog.challegram` der Reihe nach
  mit `startActivity`. Dafür sind **keine** `<queries>`-Einträge nötig: Fehlt eine App, kommt einfach eine
  `ActivityNotFoundException`. Ist keine installiert, öffnet sich der Android-Teilen-Dialog.
- `minSdk` ist 24; `PROCESS_TEXT` gibt es ab API 23.

### 10.3 Abhängigkeiten

```toml
# gradle/libs.versions.toml
bouncycastle = "1.86"
bouncycastle = { group = "org.bouncycastle", name = "bcprov-jdk18on", version.ref = "bouncycastle" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.bouncycastle)
```

Alles andere (AES-GCM, `SecureRandom`, Unicode-Normalisierung) bringt Android mit.

---

## 11. Tests

```bash
./gradlew :app:testDebugUnitTest
```

| Test | Prüft |
|---|---|
| `EncryptDecryptTest` | Hin- und Rückweg mit Umlauten, Emojis, Zeilenumbrüchen und leerem Text; Name, Geräte-Kennung und Zeit kommen an; Klartext und Name sind im Geheimtext nicht sichtbar; gleicher Text ergibt unterschiedlichen Geheimtext; die längste erlaubte Nachricht passt in 4096 Zeichen; Länge nur in 64-Byte-Stufen sichtbar; unterschiedlich getippte Sätze ergeben denselben Schlüssel. |
| `WrongPassphraseTest` | Falscher Satz und gelöschter Schlüssel liefern `WrongKey`, keine Exception. |
| `TamperingTest` | Ein verändertes Bit in Salt, Nonce, Geheimtext oder Tag und ein abgeschnittener Geheimtext werden erkannt – auch mit dem richtigen Schlüssel. |
| `TencdecMessageTest` | Parsen der eigenen Ausgabe, Text davor und danach, fremder Text, kaputte Nachrichten, neuere Version. |
| `FormatTest` | Base64url für alle Längen, Padding, innere Nutzlast, Namenslänge, Normalisierung (auch NFD), Wortzählung, Satzvergleich. |
| `WordlistTest` | Projekt-Wortliste groß genug, 8 Wörter ≥ 103 Bit, Filterregeln, zu kleine Liste wird abgelehnt, Generator, Prüfwörter. |
| `KeyDeriverTest` | Die echten Argon2-Parameter sind deterministisch und hängen vom Salt ab. |

Stand: **35 Encrogram-Tests, alle grün.** Die meisten Tests nutzen billige Argon2-Parameter, damit sie schnell bleiben.

---

## 12. Offene Punkte

- [ ] **Gerätetest:** Erscheint „Entschlüsseln“ im Textmenü von Telegram? Sonst bleibt der Weg über die Zwischenablage.
- [ ] **Gerätetest:** Sperre bei Bildschirm aus und nach 2 Minuten, `FLAG_SECURE`, Tastatur ohne Vorschläge, Senden an Telegram.
- [ ] **Argon2-Dauer auf dem Handy messen.** Ziel: Ableitung unter 2 Sekunden. Sonst Parameter anpassen – beide Seiten
      brauchen dafür dieselbe Encrogram-Version, die Parameter gehören dann in eine neue Formatversion.
- [ ] Icon auf dem Gerät begutachten und gegebenenfalls nachjustieren.
