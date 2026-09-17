# Android-Abgleich mit der Web-App – 16. September 2026

Verglichen wurden die Web-/Backend-Änderungen seit der ersten Android-Umsetzung vom 13. September mit dem lokalen Stand `56524a4`. Zusätzlich wurden die öffentlichen Live-Endpunkte für Festivals, Genres, Konzerte und Entdecken gelesen. Android-Version: **1.2, versionCode 3**.

## Wesentliche Veränderungen und Anpassungen

| Änderung in Web/Backend | Android-Anpassung |
| --- | --- |
| Personalisierter Startbereich ersetzt getrenntes Entdecken | Start als eigener Tab; kommende gemerkte Konzerte als kompakte Liste, jedes Festival nur einmal, gefolgte Bands horizontal davor; Tageskarte, Empfehlungen und weitere Entdecken-Rubriken |
| Festivals bündeln Konzerte | Übersicht, Detailseite, Anlage/Bearbeitung und Zuordnung im Konzertformular; Festivalrechte werden als EVENT_SERIES erkannt |
| Spielpläne als Liste oder Raster | Native chronologische Liste bzw. horizontal scrollbare Bühnenspalten, Gleichzeitigkeit, Ortsfilter und gespeicherter Darstellungsstil |
| Eigene Auftrittszeit je Band | Optionaler Zeitpicker pro Band; Erhalt beim Bearbeiten und bei Activity-Neuerstellung; Reihenfolge berücksichtigt Auftritte nach Mitternacht |
| Festival-Acts einzeln merken | Merken/Entmerken am Line-up, gespeicherte Acts im Profilmodell, Festivalfilter Alle/Gemerkt; Merkliste immer als Liste |
| Festival-Ticketlink hat Vorrang | Konzertdetails verwenden bevorzugt den Ticketlink des Festivals; Listen zeigen den Festivalnamen |
| Mehrere Genres als ODER-Filter | Auswahl mit Trefferzahlen, Rücksetzen und gemeinsamer kommaseparierter API-Parameter |
| Lieblingsgenres und Empfehlungen | Genres vom Server statt fest einprogrammierter Liste, Auswahl im Konto, Hinweis nach Login ohne Vorlieben; neue Genres werden automatisch übernommen |
| POST /api/events liefert jetzt EventCreateResult | Sofortige Veröffentlichung und Einreichung zur Prüfung werden getrennt behandelt; bestätigte Einreichung löst keinen falschen Detailaufruf aus |
| Eigene Vorschläge | Statusübersicht mit Prüfstatus, Ablehnungsgrund und Link zum freigegebenen Konzert |
| Temporäres Passwort muss ersetzt werden | Verpflichtender Passwortwechsel vor dem übrigen Inhalt; zusätzlich normaler Passwortwechsel im Konto |
| Absagen und Wiederaktivieren | Beide Aktionen mit Bestätigung verfügbar |
| Konzerte/Bands/Orte löschen | Bestätigungsdialog und passende EDIT-/MANAGE-Prüfung; Konflikt bei verbleibenden Konzerten wird angezeigt |
| Bilder aus Zwischenablage | Nutzerinitiierte Übernahme kopierter Android-Bild-URIs in den Upload-Feldern; Dateiauswahl bleibt verfügbar |
| Festival-Links | Unterstützte HTTPS-Links führen zur Festivalansicht, einschließlich `filter=saved` |

Die Smartphone-Navigation bleibt auf fünf Tabs begrenzt: Start, Konzerte, Festivals, Orte und Mein GigLister. Die Suche ist über die Lupe im Kopfbereich erreichbar. Mein GigLister bündelt Einstellungen und eigene Inhalte; Plattformadministration bleibt ausgeschlossen.

## Bewusste Abgrenzung

Die neuen Admin-Benutzerseiten, temporäre Passwörter vergeben, Duplikate zurückweisen, Massenveröffentlichung und administrative Einreichungsprüfung werden nicht in Android übernommen. Android unterstützt den notwendigen Passwortwechsel eines betroffenen Benutzers, aber keine Benutzeradministration. Die geänderte Vollständigkeitsbewertung für Bands wird vom Backend bestimmt; Android erzwingt weiterhin keine Stadt als Pflichtfeld für Bands.

Backend und Web-App wurden nicht verändert. Die schreibenden Abnahmen verwenden MockWebServer innerhalb der Testinstallation. Die Live-Prüfung blockiert alle HTTP-Methoden außer GET. Das physische Telefon wurde nicht verändert.

## Prüfung und Abnahmegrenzen

Nachprüfung am 17. September: Alle 14 bisherigen Bedienungstests bestanden zusammen mit den sechs neuen Story-Tests. Details zum aktuellen Build: [Stories-Abgleich](android-stories-2026-09-17.md).

Die ursprünglichen Ergebnisse stehen in `android/build/update-final-build.log`, `android/build/update-ui-tests.txt` und `android/build/update-live-ui-tests.txt`. Zusätzliche Vertragstests prüfen die zwei Einreichungsantworten, Nachtauftritte, Festival-Deduplizierung, einzelne Acts, Ticketvorrang, Genreparameter und Pflichtpasswortfelder.

Echte E-Mail-/Push-Zustellung, Live-Uploads und Änderungen mit einem echten Benutzerkonto bleiben außerhalb dieses schreibgeschützten Live-Abgleichs. Der finale Build ist eine installierbare Debug-APK, keine Store-Veröffentlichung. Automatisch verifizierte App Links benötigen weiterhin die passende Serverkonfiguration für die endgültige Signatur.

## Ansichten

- [Startbereich](android-update-2026-09-16/update-home-dark.png)
- [Festival mit Live-Daten](android-update-2026-09-16/update-festival-light.png)
- [Spielplan mit Live-Daten](android-update-2026-09-16/update-timetable-light.png)
- [Rasteransicht mit parallelen Testveranstaltungen](android-update-2026-09-16/update-grid.png)

[Android-Bau-/Testanleitung](../android/README.md) · [Erste Analyse](android-web-analyse.md)
