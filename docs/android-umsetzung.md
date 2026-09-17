# Android-Ausbau – 13. September 2026

Die App wurde auf Grundlage der Web-Analyse um die Benutzerfunktionen erweitert. Backend und Web-Oberflächen wurden nicht geändert. Plattform-Admin-Seiten und Admin-API-Aufrufe sind nicht enthalten. Rechte an eigenen bzw. freigegebenen Bands, Orten und Konzerten bleiben nutzbar.

## Umsetzung

| Bereich | Android |
| --- | --- |
| Konzerte | Gastzugang, nachladbare Liste, Zeitfilter, Monatskalender, Details, Tickets, Merken |
| Umgebung | Stadt oder GPS, Suchradius, OpenStreetMap mit Konzertzahlen und Ortsauswahl |
| Entdecken/Suche | Rubriken der Web-App und gemeinsame Treffer für Konzerte, Bands und Orte |
| Profile | Band-/Ortsdetails, Folgen, kommende Konzerte, Links und Routen |
| Persönlicher Bereich | Merkliste, gefolgte Bands, verwaltete Inhalte, eigene Konzerte, Heimatort |
| Inhalte pflegen | Konzert-/Band-/Ortsformulare, Dublettenvorschläge, Bildauswahl und Upload |
| Berechtigungen | EDIT/MANAGE getrennt, Status, Objektfreigaben und Zuständigkeitsanfragen |
| Konto | Login, Registrierung, Passwortwiederherstellung und Bestätigungslinks |
| Gestaltung | Heller/dunkler Modus, Markenschriften, diagonale Collagen, mobile Navigation |
| Push | Bestehendes FCM integriert; Konzert-ID öffnet die passende Detailseite |

Karte und Kalender laden alle Ergebnis-Seiten im gewählten Zeitraum. Bei bekannter Position wird kein zusätzlicher Stadtfilter gesendet, der Nachbarorte ausschließen würde. Konzertformulare bewahren Eingaben bei Activity-Neuerstellung. Formularaktionen führen Navigation und UI-Änderungen auf dem Android-Hauptthread aus.

## Prüfung

- Debug-APK gebaut und in einem separaten Pixel-Emulator installiert.
- Sieben lokale Tests für Datumsgrenzen, Radiusparameter, Rechte, Links, Pagination und API-Verträge bestanden.
- Sechs gemeinsame Bildschirmtests bestanden: Gastzugang/Ortswahl, Login/Merkliste, Kalender/Suche, EDIT-Begrenzung, MANAGE-Objektrechte und Konzertanlage inklusive Activity-Neuerstellung.
- Öffentlicher Live-Test für Konzertliste, Detailseite und sichtbare Karte in heller/dunkler Darstellung bestanden. Sein HTTP-Client blockiert alle Methoden außer GET.
- Bildschirmaufnahmen visuell kontrolliert. Die zunächst leere WebView-Karte erhielt eine explizite Inhaltsgröße; der Test kontrolliert auch ihre sichtbare Höhe.
- Lint ohne Fehler. Verbleibende Hinweise betreffen Bibliotheksversionen und bestehende Manifest-/Ressourcenkonfiguration.

Schreibende Abläufe wurden gegen MockWebServer geprüft. Es wurden keine Testkonzerte, Konten oder Rechte auf dem Live-Server angelegt. Das angeschlossene physische Telefon wurde nicht verändert.

## Grenzen der Abnahme

Echte Push-/E-Mail-Zustellung, Datei-Uploads sowie Konto- und Freigabeänderungen mit einem realen Benutzerkonto sind noch nicht live abgenommen. Der Debug-Build ist keine signierte Store-Veröffentlichung. Für verifizierte automatische App Links muss die Serverdatei `assetlinks.json` zur endgültigen App-Signatur passen. Manuelle Stadtsuche ohne Geocoder-Ergebnis bleibt möglich; Karte und genauer gespeicherter Heimatort benötigen Koordinaten.

## Ansichten

- [Konzerte, dunkel](android/home-dark.png)
- [Konzertdetails, dunkel](android/event-dark.png)
- [Karte, dunkel](android/map-dark.png)
- [Karte, hell](android/map-light.png)

[Bau- und Testanleitung](../android/README.md) · [Web-Bestandsaufnahme](android-web-analyse.md)
