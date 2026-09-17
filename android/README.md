# GigLister für Android

Native Kotlin-/Jetpack-Compose-App für dieselbe REST-API wie die Web-App. Der Ausbau ergänzt den Benutzerbereich; Plattformadministration ist nicht Bestandteil der App.

## Aktualisierung auf Version 1.2

Der Abgleich vom 16. September ergänzt Festivals und Spielpläne, Act-Merklisten, den persönlichen Startbereich, Genrevorlieben/-filter, Empfehlungen, Konzertvorschläge zur Prüfung, Pflichtpasswortwechsel sowie Löschen und Wiederaktivieren. Die Suche liegt jetzt in der Kopfzeile. Details, Screenshots und Abnahmeumfang: [Änderungsanalyse](../docs/android-aenderungen-2026-09-16.md).

Die Version setzt die aktuelle API voraus: `POST /api/events` liefert jetzt `EventCreateResult` mit entweder `event` oder `submission`.
## Funktionen

- Gastzugang, Konzertliste mit nachladbaren Seiten, Datumsfilter und Monatskalender.
- Manuelle Ortswahl, GPS und Suchradius; bei bekannten Koordinaten einheitliche Umkreissuche.
- Orte mit OpenStreetMap-Karte, Konzertmarkern und zugehörigen Veranstaltungen.
- Entdecken, gemeinsame Suche nach Konzerten/Bands/Orten und Detailprofile.
- Ticketlinks, Routen, Merkliste, gefolgte Bands und persönlicher Bereich.
- Registrierung, Login, E-Mail-Bestätigung und Passwortwiederherstellung über eingehende Links.
- Konzerte, Bands und Orte anlegen/bearbeiten, Bildauswahl und Upload, Dublettenvorschläge.
- Objektbezogene EDIT-/MANAGE-Rechte, Veröffentlichungsstatus und Zuständigkeitsanfragen.
- Heimatort, Suchradius und FCM-Benachrichtigungen mit Konzertnavigation.
- Helle und dunkle Gestaltung mit GigLister-Farben, Archivo Black, Barlow Condensed und diagonalen Konzertcollagen.

## Bauen

Benötigt werden JDK 17, Android SDK 34 und die projektbezogene `app/google-services.json`. Die Firebase-Datei und `local.properties` bleiben lokal. Gradle verwendet den vorhandenen Wrapper.

```powershell
# Im Verzeichnis android; JAVA_HOME auf ein installiertes JDK 17 setzen.
./gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Sie ist mit dem lokalen Debug-Schlüssel signiert. Für die Veröffentlichung ist ein eigener Release-Schlüssel erforderlich.

Beide Build-Typen verwenden standardmäßig `https://sandbox.fotosvorju.de`. Für einen lokalen Server:

```powershell
./gradlew.bat assembleDebug -Pgiglister.apiBaseUrl=http://10.0.2.2:8080
```

HTTP ist ausschließlich im Debug-Manifest freigegeben. API-Logging ist deaktiviert; Authentifizierungstoken werden nicht protokolliert.

## Prüfen

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleDebugAndroidTest
```

`MainFlowTest` verwendet einen lokalen MockWebServer innerhalb des Testprozesses. Er verändert keine Live-Konten oder Veranstaltungen. Für instrumentierte Tests einen separaten Emulator auswählen:

```powershell
adb -s <emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <emulator> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <emulator> shell am instrument -w -e class com.giglister.app.MainFlowTest com.giglister.app.test/androidx.test.runner.AndroidJUnitRunner
```

`LiveReadOnlyTest` ist standardmäßig deaktiviert. Mit `-e live true -e class com.giglister.app.LiveReadOnlyTest` prüft er öffentliche Daten und erstellt Bildschirmaufnahmen. Sein HTTP-Client blockiert alle Methoden außer GET. Die Tests setzen ausschließlich die Einstellungen der Testinstallation zurück.

## Betriebsgrenzen

- Für bestätigte Android App Links muss der Webserver eine passende `/.well-known/assetlinks.json` für Paket und Signatur bereitstellen. Die App verarbeitet die unterstützten Links bereits; automatische Browser-Weiterleitung ist ohne diese Serverkonfiguration nicht zugesichert.
- FCM benötigt zusätzlich die bestehende Firebase-Konfiguration im Backend und eine Benachrichtigungsfreigabe auf dem Gerät.
- Manuelle Stadtsuche nutzt den Android-Geocoder. Ohne Geocoder-Ergebnis funktioniert die Suche nach Stadtnamen, aber eine Karte und ein genauer gespeicherter Heimatort benötigen Koordinaten.
- Kartenorte ohne Koordinaten bleiben in der Konzertliste sichtbar und erhalten einen Hinweis.
- E-Mail-Zustellung, echte Bild-Uploads und Push-Zustellung wurden nicht gegen produktive Benutzerkonten getestet.

## Aufbau

`ui/browse` enthält Listen, Kalender, Karte und Detailansichten, `ui/edit` die Formulare und Objektberechtigungen, `ui/login` die Authentifizierung. `GigState` verwaltet Sitzung und Suchbereich. `data/api` enthält die typisierten REST-Aufrufe, `data/model` die DTOs. Leaflet und Schriftdateien liegen lokal in der App; Lizenztexte unter `app/src/main/assets/licenses`.

Die ursprüngliche Bestandsaufnahme steht in `../docs/android-web-analyse.md`.
