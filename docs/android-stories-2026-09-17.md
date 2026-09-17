# Android: Stories – Abgleich vom 17. September 2026

Die Web-Änderungen wurden anhand der aktuellen Implementierung von `BandStoryComposer`, `BandStoryViewer`, `CroppedStoryImage`, `StoryRing`, den Layer-/Farb-Helfern sowie `BandStoryController`, `BandStoryService` und den Band-/Profil-DTOs analysiert. Die öffentliche API wurde zusätzlich lesend geprüft. Die früheren Erweiterungen für Festivals, Genres und Einreichungen bleiben erhalten.

## Funktionsabgleich

| Web-Änderung | Android 1.3 |
| --- | --- |
| Stories einer Band laufen nach 24 Stunden ab | Öffentliche Story-Liste und zusätzliche Prüfung von `expiresAt`; leere/abgelaufene Stories führen zu einer verständlichen Anzeige |
| Story-Rahmen auf Startseite und Bandprofil | Farbiger eckiger Rahmen, eigenes quadratisches Profilbild; Logo wird vollständig eingepasst, Initiale als Ersatz |
| Eigenständiges Band-Profilbild | Bearbeitungsformular unterstützt Profilbild zusätzlich zu Logo und Titelbild; vorhandenes Profilbild bleibt beim Speichern erhalten |
| Story-Viewer | 9:16-Ansicht, Fortschrittssegmente, sechs Sekunden pro Story, Vor/Zurück, Halten zum Pausieren, ausdrücklicher Pause-Knopf, Pause im Hintergrund |
| Verknüpfungen im Viewer | Bandname im Kopf und Band-Tags öffnen das jeweilige Profil |
| Story erstellen | Bestehender Bild-Upload mit Dateiauswahl/Zwischenablage, Format- und 5-MB-Prüfung |
| Foto positionieren | Verschieben, Zwei-Finger-Zoom und Drehung, ergänzende Regler, Zurücksetzen, proportionaler vollständiger Bildausschnitt als Ausgangspunkt |
| Bildhintergrund | Dominante Farbe aus einem kleinen Farbhistogramm des Fotos |
| Freie Textebenen | Mehrere Ebenen, Position/Größe/Drehung, Weiß–Regenbogen–Schwarz-Farbregler, zuschaltbarer dunkler Kasten |
| Andere Bands markieren | Suchendpunkt für veröffentlichte Bands; Name/Bild als Snapshot; gleiche Transformations- und Farboptionen wie Text |
| Eigene Story entfernen | Bestätigungsdialog, anschließende Aktualisierung des Story-Bestands und Profils |
| Rechte | Erstellen/Löschen nur mit EDIT oder MANAGE für die betreffende Band; keine Plattform-Admin-Oberfläche |

## Kompatibilität und Bedienung

Android speichert dieselben Prozentkoordinaten, Rotationswinkel, Farben und JSON-Ebenen wie die Webseite. Foto und Ebenen verwenden überall einen 9:16-Rahmen; Textgrößen werden wie die Web-`cqw`-Werte relativ zur Rahmenbreite berechnet. Vorschau und Viewer teilen sich dieselbe native Darstellung. Ältere Ebenen ohne Farbe/Kasten bleiben lesbar, fehlerhafte Einträge werden übersprungen. Der Editor erhält seinen Entwurf einschließlich Ebenen und Auswahl bei Activity-Neuerstellung.

Die Story-Daten werden über `GET/POST /api/bands/{id}/stories` und `DELETE /api/bands/{id}/stories/{storyId}` verarbeitet. Band-Markierungen verwenden `GET /api/bands/search?q=…`. Web und Backend wurden nicht verändert.

## Prüfung

- Lokale Vertragstests: Web-Layer-Roundtrip, fehlerhafte/ältere JSON-Ebenen, Band-Snapshots, Farbpalette, Bildproportionen, Ablaufgrenze und Erhalt des Profilbilds im Bearbeitungs-Payload.
- Emulator-Tests: Gastansicht, Navigation/Pause/Tags, Rechte, Löschen mit Bestätigung, Story-Rahmen, Upload/Entwurfswiederherstellung/Veröffentlichen gegen MockWebServer.
- Regressionsprüfung der bisherigen Android-Abläufe und lesender Live-Test.

Ergebnis: 19 lokale Tests, alle 20 Emulator-Bedienungstests (6 Stories, 14 bisherige App-Abläufe) und der zusätzliche GET-only-Live-Test bestanden. Build und Lint erfolgreich; Lint meldet 0 Fehler und 14 bestehende Hinweise. Dabei wurde auch ein Startfehler beim direkten Öffnen eines Profil-Links korrigiert: Die Navigation wartet nun auf den initialisierten Navigationsgraphen. Die Veröffentlichung wurde mit einem lokalen Bild über die Android-Zwischenablage, echter Upload-Verarbeitung, Activity-Neuerstellung und verschobener Band-Ebene getestet. Fehlgeschlagenes Löschen zeigt den Fehler im Dialog und erlaubt Wiederholung.

Protokolle: `android/build/stories-final-build.log`, `android/build/stories-all-ui-tests.txt`, `android/build/stories-live-tests.txt`. APK: `android/app/build/outputs/apk/debug/app-debug.apk` (Version 1.3, VersionCode 4, Debug-Signatur).

Es wurden keine Stories oder Testdaten auf dem Live-Server veröffentlicht. Der öffentliche API-Abgleich bestätigt die neuen Endpunkte; für die geprüfte Band war zum Prüfzeitpunkt keine Story aktiv. Die Story-Screenshots zeigen deshalb ausdrücklich synthetische Testdaten. Live-Uploads mit einem echten Benutzerkonto und eine Store-Veröffentlichung gehören nicht zu diesem Testlauf.

## Ansichten

- [Story-Viewer mit Web-kompatiblen Ebenen](android-stories-2026-09-17/stories-viewer.png)
- [Nativer Editor mit Text und Band-Markierung](android-stories-2026-09-17/stories-editor.png)
