# Datenschutz bei SilentPort

Dies ist ein **Zero-Profit**- und **Zero-Data**-Projekt.

Wir sind der festen Überzeugung, dass Software, die dem Schutz der Privatsphäre dient, selbst ein Höchstmaß an Datenschutz bieten muss. Diese App wurde von Grund auf nach dem Prinzip "Was auf dem Gerät passiert, bleibt auf dem Gerät" entwickelt.

## Das Grundprinzip: 100% Lokale Verarbeitung

SilentPort sammelt, speichert, teilt oder überträgt **keinerlei** persönliche Daten.

Alle Berechnungen, Analysen (welche App wann genutzt wurde) und Firewall-Aktionen finden ausschließlich und zu 100% auf Ihrem Gerät statt. Es gibt keinen Server, mit dem die App kommuniziert.

## Erforderliche Berechtigungen und warum wir sie brauchen

SilentPort benötigt Berechtigungen, die sensibel erscheinen. Hier ist der genaue Grund, warum sie für die Kernfunktionalität unerlässlich sind – und wie wir sicherstellen, dass sie nicht missbraucht werden.

### 1. Lokale-Firewall (`BIND_VPN_SERVICE`)

Um den Netzwerkzugriff für andere Apps zu blockieren, nutzt SilentPort die `VpnService`-API von Android.

**Dies ist KEIN echtes VPN:**
* Es wird **niemals** eine Verbindung zu einem externen Server hergestellt.
* Ihr Netzwerkverkehr wird **niemals** umgeleitet, überwacht oder protokolliert.
* Die App erstellt einen "leeren" lokalen Tunnel auf Ihrem Gerät. Der Netzwerkverkehr von blockierten Apps wird an diesen Tunnel gesendet und dort **sofort verworfen** (`drainPackets`-Implementierung).

### 2. Nutzungsstatistiken (`PACKAGE_USAGE_STATS`)

Dies ist die absolute Kernfunktion der App.

* **Zweck:** SilentPort muss wissen, *wann* Sie eine App zuletzt verwendet haben, um festzustellen, ob sie "selten" ist.
* **Implementierung:** Wir verwenden den `UsageStatsManager` (implementiert in `UsageAnalyzer.kt`), um *ausschließlich* den Zeitstempel der letzten Nutzung einer App abzufragen.
* **Datenspeicherung:** Diese Informationen (App-Name, letzter Zeitstempel) werden **nur lokal** in der App-Datenbank (`AppDatabase`) auf Ihrem Gerät gespeichert.

### 3. App-Liste (`QUERY_ALL_PACKAGES`)

* **Zweck:** Erforderlich, um Ihnen eine vollständige Liste aller installierten Anwendungen anzuzeigen, die von der Firewall verwaltet werden können.
* **Datenspeicherung:** Diese Liste wird nur zur Laufzeit und in der lokalen Datenbank (siehe oben) verwendet.

### 4. Vordergrunddienst (`FOREGROUND_SERVICE`)

* **Zweck:** Dies ist eine technische Anforderung von Android. Damit der `VpnService` (die Firewall) zuverlässig im Hintergrund laufen kann, muss er als Vordergrunddienst mit einer persistenten Benachrichtigung deklariert werden.

## Unser "Zero Data"-Versprechen (Technische Beweise)

Wir behaupten nicht nur, keine Daten zu sammeln, wir haben es technisch sichergestellt:

1.  **Keine Tracker oder Werbe-SDKs:** Die App enthält absolut keine Drittanbieter-Bibliotheken für Tracking, Werbung oder Analyse. Dies ist in der Build-Datei (`app/build.gradle.kts`) ersichtlich.
2.  **Keine Cloud-Backups:** Wir haben die automatische Cloud-Sicherung von Android für die App-Daten (die Ihre Nutzungsmuster enthalten) **explizit deaktiviert**. Selbst wenn Sie Google-Backups nutzen, werden die Daten von SilentPort nicht in die Cloud hochgeladen.
3.  **Keine Netzwerk-Berechtigung (außer für VPN):** Die App selbst (außer der VPN-Dienst) fordert keine Internet-Berechtigung an, um Daten zu senden.

## Open Source und Transparenz

SilentPort ist vollständig Open Source unter der GPL-Lizenz. Jeder kann den Quellcode einsehen, um alle hier gemachten Aussagen unabhängig zu überprüfen.

**Zusammenfassend: Wir lesen nur die minimal notwendigen Daten (App-Liste, letzter Zeitstempel), um die Kernfunktion zu erfüllen. Alle diese Daten verlassen niemals Ihr Gerät.**
