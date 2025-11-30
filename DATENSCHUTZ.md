# Datenschutzerklärung für SilentPort

**Stand:** November 2025  
**Version:** 1.0

SilentPort ist ein **Zero-Profit**- und **Zero-Data**-Projekt. Wir glauben, dass Privatsphäre-Tools selbst den höchsten Standard an Datensparsamkeit erfüllen müssen.

Diese Datenschutzerklärung dient nicht nur der rechtlichen Absicherung, sondern als technischer Beleg für unser Versprechen: **"Was auf Ihrem Gerät passiert, bleibt auf Ihrem Gerät."**

---

## 1. Das Kernprinzip: 100% Lokale Verarbeitung

SilentPort sammelt, speichert, teilt oder überträgt **keinerlei** personenbezogene Daten an externe Server.

* **Keine Server-Kommunikation:** Die App kommuniziert mit keinem Backend. Es gibt keine Login-Server, keine Analyse-Server und keine Werbe-Netzwerke.
* **Lokale Datenbank:** Alle Nutzungsstatistiken werden in einer isolierten Datenbank (`AppDatabase`) auf Ihrem Gerät gespeichert, auf die andere Apps keinen Zugriff haben.

---

## 2. Technische Analyse der Berechtigungen

SilentPort fordert Berechtigungen an, die auf den ersten Blick sensibel wirken. Hier erklären wir technisch transparent, wofür diese notwendig sind und wie wir Missbrauch ausschließen.

### 2.1 Lokale Firewall (`BIND_VPN_SERVICE`)
Dies ist die Kernfunktion der Firewall. Android erfordert die Nutzung der VPN-Schnittstelle, um Netzwerkverkehr zu filtern.

* **Funktionsweise:** Die App erstellt eine *lokale Loopback-Schnittstelle* auf Ihrem Gerät.
* **Kein echtes VPN:** Es wird **niemals** eine Verbindung zu einem externen VPN-Server (Tunnel) aufgebaut. Ihre IP-Adresse wird nicht verschleiert oder umgeleitet.
* **Traffic-Behandlung:**
    * **Blockierte Apps:** Pakete werden an einen lokalen "Sinkhole"-Socket gesendet und dort sofort verworfen (`drainPackets()`-Methode im Quellcode).
    * **Erlaubte Apps:** Der Verkehr fließt direkt und unverändert am Filter vorbei. SilentPort inspiziert oder protokolliert den Inhalt (Payload) der Datenpakete nicht.

### 2.2 Nutzungsstatistiken (`PACKAGE_USAGE_STATS`)
Notwendig, um ungenutzte Apps zu identifizieren.

* **Was wir lesen:** Wir fragen beim Android-System (`UsageStatsManager`) ausschließlich den Zeitstempel der letzten Nutzung ab (`MOVE_TO_FOREGROUND`).
* **Was wir NICHT lesen:** Wir sehen nicht, was Sie in der App tun, welche Inhalte Sie ansehen oder mit wem Sie kommunizieren.
* **Speicherung:** Nur App-Paketname (z.B. `com.example.app`) und der letzte Zeitstempel werden lokal gespeichert.

### 2.3 App-Liste (`QUERY_ALL_PACKAGES`)
Erforderlich, um eine Liste aller installierten Apps anzuzeigen, damit Sie diese verwalten können. Diese Daten verlassen niemals den RAM Ihres Geräts während der Laufzeit der App.

### 2.4 Internetzugriff (`INTERNET`)
Diese Berechtigung ist im Manifest deklariert, wird aber von der aktuellen App-Logik **nicht aktiv genutzt**, um Daten zu senden.

* **Sicherheits-Garantie:** Der Quellcode der App ist offen. Sie können überprüfen, dass keine Tracker-Bibliotheken oder API-Aufrufe an externe URLs existieren.

---

## 3. Unser "Zero Data"-Sicherheitsversprechen

Wir behaupten Datenschutz nicht nur, wir garantieren ihn durch technische Maßnahmen, die Sie überprüfen können:

1.  **Keine Tracker oder Werbung:**
    Ein Blick in unsere `build.gradle.kts` Datei beweist: Es sind **keine** Bibliotheken von Facebook, Google Analytics, Firebase Crashlytics oder Werbenetzwerken enthalten.

2.  **Deaktiviertes Cloud-Backup:**
    Wir haben Androids automatisches Cloud-Backup für SilentPort explizit deaktiviert (`data_extraction_rules.xml`). Das bedeutet: Selbst wenn Sie Google Drive Backups für Ihr Telefon nutzen, werden Ihre Nutzungsprofile von SilentPort **nicht** in die Cloud hochgeladen.

3.  **Open Source (GPLv3):**
    Der vollständige Quellcode ist öffentlich. Jeder Sicherheitsexperte kann verifizieren, dass keine Hintertüren existieren.

---

## 4. Ihre Rechte (DSGVO / GDPR Compliance)

Da wir keine Daten sammeln, müssen wir keine komplexen Prozesse zur Datenlöschung anbieten. Sie haben dennoch volle Kontrolle:

* **Recht auf Auskunft & Datenübertragbarkeit:** Da alle Daten lokal sind, besitzen Sie diese bereits physisch auf Ihrem Gerät.
* **Recht auf Löschung:** * Deinstallieren Sie die App, und alle Daten sind **sofort und unwiderruflich** gelöscht.
    * Alternativ: "Einstellungen > Apps > SilentPort > Speicher > Daten löschen".
* **Widerspruchsrecht:** Sie können jede Berechtigung (z.B. Zugriff auf Nutzungsdaten) jederzeit in den Android-Einstellungen entziehen. Die App wird dann in ihrer Funktionalität eingeschränkt sein, aber weiterhin respektieren, dass Sie keine Daten teilen möchten.

---

## 5. Kontakt

SilentPort ist ein Community-Projekt. Bei Fragen zur Sicherheit oder zum Quellcode können Sie uns über unser öffentliches Repository kontaktieren.

**Verantwortlich:** zerox80

**Projekt-URL:** https://github.com/zerox80/SilentPort
