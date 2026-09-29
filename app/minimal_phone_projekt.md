# Minimal Phone – Projektkonzept

## 1. Grundidee

Ein älteres Android-Smartphone soll zu einem modernen, minimalistischen **Klapphandy** umgebaut werden.

Das Smartphone behält dabei die Vorteile eines modernen Android-Geräts – insbesondere Messenger, Kamera, Kalender und E-Mail – soll aber bewusst auf wenige notwendige Funktionen reduziert werden.

Unterhalb des Smartphones wird eine **physische Zahlentastatur** angebracht, die nach unten aufgeklappt werden kann. Dadurch verbindet das Gerät die Bedienung klassischer Handys mit ausgewählten modernen Smartphone-Funktionen.

Das Projekt besteht aus drei weitgehend getrennten Komponenten:

1. **Android-Software / eigener Launcher**
2. **Physische Tastatur mit Mikrocontroller**
3. **Klappbares Gehäuse mit Scharnier**

---

## 2. Ziel

Das fertige Gerät soll sich nicht mehr wie ein gewöhnliches Smartphone anfühlen, sondern wie ein bewusst reduziertes Mobiltelefon.

Der Benutzer soll im Alltag nur Zugriff auf eine kleine Auswahl wichtiger Apps haben:

- WhatsApp
- Snapchat
- E-Mail
- Notizen
- Kalender
- Kamera
- Einstellungen

Apps wie Browser, YouTube, Instagram, TikTok, Spiele usw. sollen auf der normalen Oberfläche nicht angeboten werden.

Die vorhandenen Original-Apps werden weiterhin verwendet. WhatsApp oder Snapchat müssen also **nicht nachgebaut** werden. Der eigene Launcher startet lediglich die bereits installierten Android-Apps.

Langfristig soll ein großer Teil der Bedienung auch über die physische Tastatur möglich sein.

---

## 3. Android-Software

### 3.1 Eigener Launcher

Die Android-App wird nicht nur eine normale App, sondern soll als **Home-/Launcher-App** eingerichtet werden.

Dadurch ersetzt sie den normalen Android-Homescreen.

Beispiel:

```text
┌──────────────────────────┐
│  17:52             78 %  │
│  Dienstag, 29. September │
│                          │
│  WhatsApp     Snapchat   │
│                          │
│  E-Mail       Kalender   │
│                          │
│  Notizen      Kamera     │
│                          │
│       Einstellungen      │
└──────────────────────────┘
```

Die Oberfläche soll möglichst schlicht sein und sich auch mit wenigen Hardware-Tasten bedienen lassen.

### 3.2 Entwicklung

Geplanter Stack:

- Android Studio
- Kotlin
- Jetpack Compose
- Android SDK
- Gradle
- ADB zum Installieren und Debuggen auf dem Testgerät

Die App wird zunächst privat verwendet und kann direkt per ADB bzw. als APK auf dem eigenen Android-Gerät installiert werden. Eine Veröffentlichung im Google Play Store ist für den Prototyp nicht notwendig.

### 3.3 App-Start

Der Launcher startet installierte Apps über Android-Intents bzw. deren Packages.

Prinzip:

```text
Minimal Phone Launcher
        │
        ├── WhatsApp
        ├── Snapchat
        ├── E-Mail
        ├── Kalender
        ├── Notizen
        ├── Kamera
        └── Android-Einstellungen
```

### 3.4 Einschränkung des Geräts

In einer ersten Version reicht es, nur die gewünschten Apps auf dem eigenen Launcher anzuzeigen.

Später kann untersucht werden, das Gerät stärker einzuschränken, beispielsweise über Androids **Lock Task / Dedicated Device**-Funktionen.

Damit könnte das Gerät stärker auf die ausgewählten Anwendungen beschränkt werden.

### 3.5 Einstellungen

Es soll zwei Arten von Einstellungen geben.

**Android-Systemeinstellungen** für beispielsweise:

- WLAN
- Bluetooth
- Mobilfunk/SIM
- Display
- Ton
- Akku

**Eigene Minimal-Phone-Einstellungen** könnten später enthalten:

- T9 ein/aus
- Tastenbelegung
- Tastenton
- Vibration
- USB-/Bluetooth-Tastatur
- Verhalten beim Auf- und Zuklappen
- Dark Mode
- Schriftgröße
- erlaubte Apps

---

## 4. Physische Tastatur

### 4.1 Grundaufbau

Unterhalb des Smartphones soll eine klassische Handy-Zahlentastatur angebracht werden.

Geplanter Aufbau:

```text
          ↑
      ←   OK   →
          ↓

      1    2    3
     ABC  DEF

      4    5    6
     GHI  JKL  MNO

      7    8    9
    PQRS  TUV  WXYZ

      *    0    #

    Back       Home
```

Neben den Zahlentasten sind Navigationstasten sinnvoll, damit der Launcher möglichst weitgehend ohne Touchscreen bedient werden kann.

### 4.2 Mikrocontroller

Als aktueller Favorit wurde der **Seeed Studio XIAO ESP32-S3** ausgewählt.

Gründe:

- sehr klein (ca. 21 × 17,8 mm)
- USB-C
- natives USB
- kann als USB-HID-Gerät verwendet werden
- genügend GPIOs für eine Tastenmatrix
- zusätzlich Bluetooth Low Energy verfügbar
- günstig
- Arduino-/ESP32-Ökosystem

Dadurch bleiben zwei Kommunikationsmöglichkeiten offen:

```text
                ESP32-S3
               /        \
              /          \
        USB-HID          BLE
           │              │
           └──── Android ─┘
```

Für den ersten Aufbau wird **USB-HID bevorzugt**.

---

## 5. Verbindung zwischen Tastatur und Android

### 5.1 USB-HID

Der ESP32-S3 soll sich gegenüber Android wie eine normale USB-Tastatur verhalten.

```text
Taste drücken
     ↓
ESP32-S3
     ↓
USB HID Keyboard Event
     ↓
Android
     ↓
Launcher / geöffnete App
```

Vorteile:

- kein Bluetooth-Pairing
- geringe Latenz
- stabile Verbindung
- kein eigener Akku für die Tastatur notwendig
- Android unterstützt Standard-HID-Tastaturen bereits

Das Android-Smartphone kann den Controller über USB mit Strom versorgen.

### 5.2 Alternative Bluetooth

Da der ESP32-S3 zusätzlich BLE unterstützt, kann später optional eine Bluetooth-HID-Verbindung implementiert werden.

Das ist insbesondere für Prototypen oder alternative Gehäusevarianten interessant.

---

## 6. Tastenmatrix

Die Tasten sollen nicht einzeln jeweils einen GPIO benötigen, sondern als Matrix verschaltet werden.

Beispiel einer 5×4-Matrix:

```text
       C1   C2   C3   C4

R1     1    2    3    ↑
R2     4    5    6    ↓
R3     7    8    9    ←
R4     *    0    #    →
R5    Back  OK  Home   Fn
```

Damit können bis zu 20 Tasten mit nur

- 5 Row-Leitungen
- 4 Column-Leitungen

also insgesamt **9 GPIOs** abgefragt werden.

---

## 7. Taster

Für das endgültige Gerät sind klassische mechanische Keyboard-Switches wahrscheinlich zu dick.

Bevorzugt werden deshalb **flache SMD-Tactile-Switches** mit einer darüberliegenden Kunststoff- oder Silikontastatur.

Prinzip:

```text
     Kunststofftaste
           ↓
      ┌─────────┐
      │    5    │
      └────┬────┘
           │
         ┌─┴─┐
PCB ─────┤ ● ├─────
         └───┘
       SMD-Taster
```

Ziel ist ein möglichst dünnes Tastaturmodul von grob **6–8 mm**, sofern Mechanik, PCB und Taster dies ermöglichen.

Für den ersten Prototyp können normale Taster bzw. ein Breadboard verwendet werden.

---

## 8. Klappmechanismus und Gehäuse

Die Tastatur wird unter dem Smartphone befestigt und kann nach unten aufgeklappt werden.

```text
GESCHLOSSEN

┌────────────────┐
│   Smartphone   │
└────────────────┘
┌────────────────┐
│    Tastatur    │
└────────────────┘


GEÖFFNET

┌────────────────┐
│                │
│   Smartphone   │
│                │
└────────────────┘
        ○  Scharnier
        │
        │
┌────────────────┐
│ ↑              │
│ ← OK →         │
│ 1 2 3          │
│ 4 5 6          │
│ 7 8 9          │
│ * 0 #          │
└────────────────┘
```

Das Gehäuse kann später individuell per **3D-Druck** gefertigt werden.

Wichtig ist, dass der USB-C-Anschluss des Smartphones möglichst nicht die mechanischen Kräfte des Klappmechanismus aufnehmen muss.

Denkbar sind deshalb:

- kurzes flexibles USB-Kabel
- interne USB-Leitung
- später FPC/Flex-Kabel durch das Scharnier

---

## 9. Auf-/Zuklappen erkennen

Als spätere Erweiterung kann ein **Hall-Sensor + Magnet** verwendet werden.

Damit kann der Mikrocontroller erkennen, ob die Tastatur geöffnet oder geschlossen ist.

Beispielsweise:

```text
Klappe öffnen
     ↓
Hall-Sensor
     ↓
ESP32-S3
     ↓
Android
     ↓
Display / Launcher aktivieren
```

Beim Schließen könnte entsprechend das Display ausgeschaltet bzw. das Gerät gesperrt werden.

---

## 10. T9 als spätere Erweiterung

Die Zahlentastatur könnte langfristig auch für Texteingaben verwendet werden.

```text
2 = ABC
3 = DEF
4 = GHI
5 = JKL
6 = MNO
7 = PQRS
8 = TUV
9 = WXYZ
```

Die intelligente Texteingabe sollte vorzugsweise auf Android und nicht direkt auf dem ESP32 implementiert werden.

Eine spätere eigene Android-IME könnte beispielsweise:

- T9-Eingabe
- Wörterbuch
- Wortvorschläge
- Autovervollständigung
- Groß-/Kleinschreibung

bereitstellen.

---

## 11. Geplante Gesamtarchitektur

```text
┌─────────────────────────────────────┐
│          ANDROID-SMARTPHONE         │
│                                     │
│       Minimal Phone Launcher        │
│                                     │
│ WhatsApp │ Snapchat │ E-Mail        │
│ Kalender │ Notizen  │ Kamera        │
│ Einstellungen                       │
│                                     │
│ Kotlin + Jetpack Compose            │
└─────────────────┬───────────────────┘
                  │
             USB-C / USB-HID
                  │
┌─────────────────▼───────────────────┐
│             ESP32-S3                │
│                                     │
│  USB HID Controller                 │
│  Tastenmatrix-Scanning              │
│  optional BLE                       │
│  optional Hall-Sensor               │
└─────────────────┬───────────────────┘
                  │
             5×4 Matrix
                  │
┌─────────────────▼───────────────────┐
│         PHYSISCHE TASTATUR          │
│                                     │
│ Navigation + 0–9 + * + #            │
│ Back / OK / Home / Fn               │
└─────────────────────────────────────┘
```

---

## 12. Sinnvolle Entwicklungsreihenfolge

### Phase 1 – Android-Prototyp

1. Android-Studio-Projekt erstellen
2. Kotlin + Jetpack Compose verwenden
3. minimalistischen Homescreen erstellen
4. WhatsApp, Snapchat, E-Mail, Kalender, Notizen, Kamera und Einstellungen starten
5. App als Android-Launcher einrichten
6. Navigation mit einer normalen USB-/Bluetooth-Tastatur testen

### Phase 2 – Hardware-Prototyp

1. XIAO ESP32-S3 beschaffen
2. einige Taster auf Breadboard anschließen
3. USB-HID-Firmware entwickeln
4. Tastendrücke an Android senden
5. Navigation im eigenen Launcher testen
6. vollständige 5×4-Tastenmatrix aufbauen

### Phase 3 – Erweiterungen

1. Tastenbelegung optimieren
2. Hall-Sensor integrieren
3. Verhalten beim Auf-/Zuklappen implementieren
4. T9-/Texteingabe entwickeln
5. eigene Minimal-Phone-Einstellungen ergänzen

### Phase 4 – endgültige Hardware

1. Maße des verwendeten Android-Smartphones übernehmen
2. eigene Tastatur-PCB entwerfen
3. flache SMD-Taster auswählen
4. Scharnier konstruieren
5. USB-/Flex-Verbindung integrieren
6. Gehäuse als 3D-Modell konstruieren
7. Gehäuse drucken und Elektronik integrieren

---

## 13. Aktueller Stand / Entscheidungen

| Bereich | Aktuelle Entscheidung |
|---|---|
| Plattform | Android |
| Entwicklungsumgebung | Android Studio |
| Sprache | Kotlin |
| UI | Jetpack Compose |
| App-Typ | eigener Android-Launcher |
| Projektname (vorläufig) | Minimal Phone |
| Apps | WhatsApp, Snapchat, E-Mail, Notizen, Kalender, Kamera, Einstellungen |
| Mikrocontroller | XIAO ESP32-S3 |
| Hauptverbindung | USB-HID |
| Alternative Verbindung | Bluetooth LE |
| Tastatur | ca. 5×4-Matrix |
| Taster | später flache SMD-Taster |
| Gehäuse | individuell / 3D-Druck |
| Klappenerkennung | optional Hall-Sensor |
| Texteingabe | später T9 / eigene Android-IME |

---

## 14. Leitgedanke

**Minimal Phone soll kein neues Smartphone von Grund auf entwickeln.**

Stattdessen werden vorhandene und bewährte Komponenten kombiniert:

- gebrauchtes Android-Smartphone als Rechenplattform, Display, Kamera und Mobilfunkmodem
- bestehende Android-Apps für Kommunikation und Alltag
- eigener minimalistischer Launcher als reduzierte Benutzeroberfläche
- ESP32-S3 als Controller für die physische Tastatur
- USB-HID als einfache Standardschnittstelle
- individuell gefertigtes Klappgehäuse als Verbindung zwischen moderner Smartphone-Hardware und klassischer Handy-Bedienung

Dadurch bleibt das Projekt technisch realistisch und kann schrittweise vom reinen Software-Prototyp bis zum vollständigen physischen Klapphandy entwickelt werden.
