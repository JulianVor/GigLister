# GigLister: Web-Analyse und Android-Zielumfang

Stand: 12. September 2026. Erste Analyse vor der Android-Erweiterung.

## Ergebnis und Prüfgrundlage

GigLister ist ein lokaler Konzertführer mit drei verknüpften Inhalten: Konzerten, Bands und Veranstaltungsorten. Dazu kommen persönliche Merklisten, gefolgte Bands und die Pflege eigener Inhalte. Die Android-App besitzt bereits eine native Grundlage, bildet aber nur einen kleinen Teil davon ab. Der Ausbau kann auf dem gemeinsamen Spring-Boot-Backend aufsetzen.

Geprüft wurden die drei bereitgestellten Screenshots, die öffentliche Live-Web-App (Konzertliste, Konzertdetails und Entdecken), die Web-Seiten und Komponenten im Repository sowie Android-Navigation, Screens, Modelle, API-Anbindung und relevante Backend-Controller und Rechteprüfungen. Angemeldete Schreibabläufe wurden anhand des Codes analysiert, nicht auf dem Server ausgeführt. Android wurde in dieser Analyse weder gebaut noch auf einem Gerät getestet. Angaben zur Android-App beschreiben den vorliegenden Quellcode, nicht eine unabhängig geprüfte installierte APK.

## Funktionsmatrix

| Bereich | Vorhandene Web-Funktion | Android im Repository | Ziel |
| --- | --- | --- | --- |
| Öffentlicher Einstieg | Konzerte, Orte, Bands, Suche und Entdecken ohne Login | Einstieg über Login bei fehlendem Token | Gastmodus; Anmeldung erst für persönliche oder schreibende Aktionen |
| Konzertliste | Nach Tagen gruppierte Bildkarten; Datum, Uhrzeit, Titel/Line-up, Ort; Folgeseiten | Nicht anklickbare Textkarten, erste maximal 50 Ergebnisse | Bildkarten, Tagesgruppen, Detailnavigation, Nachladen |
| Standort | Stadt eingeben oder Gerätestandort; 10/25/50 km; gespeicherte Vorgaben | Liste verlangt Standortfreigabe und GPS; Suchradius fest 25 km | Manuelle Ortswahl und GPS, veränderbarer Radius, gemeinsame Suchpräferenzen |
| Zeitfilter | Heute, Morgen, Wochenende, Diese Woche; explizite Datumsbereiche | API-Parameter vorhanden, keine Bedienelemente | Einheitlicher Zeitraum für Liste, Karte und Kalender |
| Kalender | Monatswechsel, Konzertanzahl je Tag, Konzertliste des Monats | Fehlt | Monatsansicht mit Tagesauswahl und zugehörigen Konzerten |
| Orte/Karte | Ohne Koordinaten Ortsliste; mit Koordinaten Konzertkarte und Liste | Fehlt | Kartenansicht mit Veranstaltungsorten, Anzahlen, Konzertzugriff und Routenplanung |
| Konzertdetails | Bild, Datum/Zeit, Beschreibung, Tickets, Merken, Line-up, Adresse, weitere Konzerte am Ort, Absagehinweis | Fehlt | Vollständiger nativer Detailbildschirm |
| Bandprofil | Foto/Logo, Name, Herkunft, Genres, Beschreibung, Website, kommende Konzerte, Folgen | Fehlt | Profil und Folgen/Entfolgen |
| Ortsprofil | Bild, Name, Adresse, Website, Route, kommende und nach Jahren gruppierte vergangene Konzerte | Fehlt | Profil einschließlich Konzerthistorie |
| Entdecken | Heute in der Nähe, Wochenende, neue Konzerte, aktive Orte, bald spielende Bands | Fehlt | Dieselben fünf redaktionell benannten Bereiche |
| Suche | Gemeinsame Suche nach Konzerten, Bands und Orten; gruppierte Ergebnisse | Fehlt | Gemeinsame Suche mit Navigation zu allen drei Inhaltstypen |
| Konto | Login, Registrierung, Nutzername-Verfügbarkeit, E-Mail-Bestätigung, Passwort vergessen/zurücksetzen, Logout | Login, Registrierung mit Bestätigungshinweis, gespeicherter Token, Logout | Fehlende Kontoflüsse und sinnvolle Rückkehr nach Anmeldung ergänzen |
| Mein GigLister | Standort, gemerkte Konzerte, gefolgte Bands samt nächstem Termin, eigene Bands, Band-Veranstaltungen, eigene Orte | Kontoanzeige und Speicherung von GPS-Position/Radius | Vollständiger persönlicher Bereich |
| Konzert anlegen | Datum, Zeit, Location, mehrere Bands, optional Titel, Ticketlink, Bild, Bildmodus, Beschreibung | Fehlt | Nativer Erstellungsablauf mit Validierung und Bildauswahl |
| Bestehende Inhalte bearbeiten | Konzertbearbeitung nach Rechteprüfung; Band-/Ortspflege einschließlich Bilder und Status | Fehlt | Bearbeitung für berechtigte Nutzer |
| Neue Bands/Orte | Eigenständige Formulare sowie Anlage während Konzerterfassung; Vorschläge gegen Duplikate | Fehlt | Beide Anlagewege berücksichtigen |
| Profil beanspruchen | Angemeldete Nutzer beantragen die Verwaltung einer unbeanspruchten Band/Location | Fehlt | Antrag senden; Entscheidung bleibt bei Web-Administration |
| Mitverwalter | MANAGE-Berechtigte vergeben/entziehen EDIT- oder MANAGE-Rechte am eigenen Objekt | Fehlt | Objektbezogene Verwaltung für berechtigte Nutzer |
| Push | Backend-Unterstützung für neue Konzerte im gespeicherten Umkreis | FCM, Gerätetoken und Benachrichtigungscode vorhanden | Erhalten, auf Geräten prüfen, Zielkonzert direkt öffnen |

## Abgrenzung: Nutzerpflege und Administration

Nicht in die Android-App gehören Admin-Dashboard, globale Inhaltsübersichten für alle Status, Benutzer-Promotion/-Demotion, globale Duplikatsprüfung und Zusammenführung, Genehmigung/Ablehnung von Claims, externe Einreichungswarteschlange und administrative Geokodierung. Die externe GPT-Einreichungsintegration ist ebenfalls kein Nutzerbereich der Android-App.

Weiterhin zum Nutzerumfang gehören das Anlegen von Konzerten, Bands und Orten sowie die Pflege eigener/berechtigter Inhalte. Dass eigenständige Erstellungsseiten teilweise aus Admin-Übersichten verlinkt sind, macht sie nicht zu exklusiven Admin-Funktionen: Die Seiten verlangen eine Anmeldung; die Erstellungsendpunkte stehen angemeldeten Nutzern offen.

Rechte laut Backend:

- Konzert bearbeiten: Ersteller oder EDIT/MANAGE an der beteiligten Location oder mindestens einer beteiligten Band.
- Band/Ort bearbeiten: EDIT oder MANAGE am jeweiligen Objekt.
- Band-/Ortsstatus ändern und Mitverwalter verwalten: MANAGE am jeweiligen Objekt.
- Unbeanspruchtes Profil anfragen: angemeldeter Nutzer; Freigabe erfolgt durch die Administration außerhalb der App.
- Öffentliche Band-/Ortsprofile: veröffentlichte Inhalte. Nicht veröffentlichte Profile sind für Gäste verborgen; angemeldete Nutzer können sie laut Controller aufrufen. `linkable` und Sitzung müssen bei Links berücksichtigt werden.

Android sollte Aktionen aus diesen Objektberechtigungen ableiten. Ein Plattform-Admin-Konto soll dort keine zusätzlichen globalen Verwaltungsoberflächen erhalten. Die serverseitigen Rechteprüfungen bleiben maßgeblich.

## Gestaltung der Web-App

Die visuelle Identität erinnert an Konzertplakate: kräftige Überschriften, schmale Metadaten-Schrift, große Bandbilder und klare, dünne Umrandungen. Die Oberfläche ist zurückhaltend, damit Fotos und Line-ups wirken.

| Bestandteil | Im Web-Code |
| --- | --- |
| Dunkler Hintergrund / Text | `#15130f` / `#f3f0e8` |
| Dunkle Oberfläche / Trennlinien | `#1d1a14` / `#322e25` |
| Sekundärer Text dunkel | `#a49c8a` |
| Akzent dunkel / hell | `#ff5a2e` / `#e8481c` |
| Heller Hintergrund / Text | `#f7f5ef` / `#16140f` |
| Überschriften | Archivo Black |
| Datum, Genres, Navigation | Barlow Condensed |
| Fließtext | Inter |

Die Web-App unterstützt helle und dunkle Systemdarstellung. Android übernimmt bereits einen Teil der Farben, verwendet aber noch die Standardtypografie und Standardformen von Material 3.

Bildlogik: Ein eigenes Konzertbild hat Vorrang. Andernfalls werden bis zu vier Bands als diagonale Segmente dargestellt. Der Modus PHOTO/LOGO beeinflusst, ob Bandfotos oder Logos/Farbflächen verwendet werden. Fehlende Fotos werden durch Farbflächen beziehungsweise ein abgedunkeltes Ortsbild aufgefangen. Die Detailseite verwendet eine abgewandelte Darstellung ohne die Bandbeschriftung der Listenkarten und lässt unter bestimmten bildlosen Bedingungen das Banner weg. Diese Regeln sollten als gemeinsame Android-Komponente umgesetzt werden.

Vorgeschlagene mobile Gestaltung:

- Fünf Hauptziele unten: Konzerte, Orte, Entdecken, Suche, Mein GigLister. Kalender als Ansicht innerhalb von Konzerte.
- Ortswahl als kompakte, gut erreichbare Schaltfläche; Stadt und Radius in einem Dialog oder Bottom Sheet.
- Konzertkarten mit markanter Bildcollage, deutlich erkennbarem Datum und mehrzeiligem Titel. Auf schmalen Geräten dürfen Bildbeschriftungen nicht den vollständigen lesbaren Line-up-Text ersetzen.
- Detailseite mit gut erreichbaren Aktionen für Tickets und Merken; Band- und Ortsprofile klar verlinken.
- Konzert anlegen als sichtbare Aktion für angemeldete Nutzer; längere Eingabe in überschaubare Abschnitte teilen.
- Markenfarben und Typografie erhalten, Touchflächen, Schriftvergrößerung, TalkBack, Ladezustände, Fehlermeldungen und fehlende Bilder von Anfang an berücksichtigen.

## Befunde, die beim Ausbau nicht übersehen werden dürfen

1. **GPS ist momentan eine Zugangshürde.** Ohne Freigabe lädt Android keine Konzertliste; ohne GPS-Ergebnis endet der Ablauf mit einem Fehler. Das Web kann ohne Standort starten und eine Stadt manuell filtern.
2. **Profilradius und Suchradius sind entkoppelt.** Das Android-Profil speichert 10/25/50 km, aber `EventsViewModel` verwendet weiterhin seinen eigenen Standard von 25 km. Suchort und gespeicherter Heimatort brauchen nachvollziehbare Zustände und sinnvolle Startwerte.
3. **Web-Listen und Karte können unterschiedliche Umgebungstreffer liefern.** Bei manueller Stadtwahl übergeben Liste/Kalender Stadtname plus Koordinaten; das Backend kombiniert Stadtnamen und Radius mit UND. Die Karte unter `/orte` lässt den Stadtnamen bewusst weg. Ein Konzert im Nachbarort kann dadurch auf der Karte erscheinen und in der Liste fehlen. Für Android einheitlich festlegen und testen, vorzugsweise reine Radiusfilterung, sobald Koordinaten bekannt sind.
4. **Es gibt feste Ergebnisgrenzen.** Android lädt höchstens 50 Konzerte ohne Folgeseite; die Web-Karte fordert 200 und der Monatskalender 300 an, jeweils ohne weiteres Nachladen. Diese Grenzen dürfen bei der Erweiterung nicht als vollständige Datenmenge interpretiert werden.
5. **„Diese Woche“ bedeutet aktuell sieben Tage ab heute.** Es ist im Web-Code kein Kalenderwochenfilter. Die Standardliste reicht bis ein Jahr ab heute. Die Android-Datumslogik muss bewusst dazu passen oder die Beschriftung muss eine abweichende Logik klar erklären.
6. **API-Funktion ist nicht gleich sichtbare Web-Funktion.** Eine Konzertabsage ist im Backend und als Server Action vorhanden, im untersuchten Konzertformular aber nicht als Bedienelement eingebunden. Der Absagehinweis auf der Detailseite ist vorhanden. Suchtypen sind in der API möglich; die sichtbare Web-Suche ist eine gemeinsame Suche. Solche Erweiterungen separat vom belegten UI-Umfang behandeln.
7. **EDIT und MANAGE sauber unterscheiden.** Die Web-Hilfsfunktion `canManageEntity` lässt auch EDIT zum Bearbeitungsformular durch. Dort wird ein Statusfeld angezeigt, während das Backend dafür MANAGE verlangt. Android sollte die Statusaktion entsprechend begrenzen.
8. **Push öffnet bislang nur die App.** Der vorhandene Notification-Intent enthält keine Konzertnavigation. Berechtigungsanfragen für Standort und Benachrichtigungen sind im Konzertscreen gekoppelt; sie sollten im passenden Nutzungskontext getrennt erfolgen.
9. **Dokumentation ist teilweise veraltet.** Die Root-README beschreibt frühere Collagen; der aktuelle Code zeichnet diagonale Streifen. Vorwärts-Geokodierung von Stadtnamen ist vorhanden. Die Android-README nennt den Emulator als Debug-Ziel, die aktuelle Gradle-Konfiguration verwendet aber auch für Debug die Live-Domain. Eine `google-services.json` existiert lokal; ihr Inhalt wurde für diese Analyse nicht benötigt.
10. **Android-Baustatus ist noch offen.** Die README bezeichnet das Projekt als ursprünglich ungebaut; daraus lässt sich der heutige Zustand nicht ableiten. Vor Umsetzung einen reproduzierbaren Build und einen Lauf auf Emulator/Gerät herstellen.

Nicht als vorhandene Web-Funktion belegt: Genre-Filter für Konzertlisten, Entfernungsangaben pro Konzert, Offline-Verfügbarkeit oder personalisierte Empfehlungen. Genres sind als Banddaten vorhanden. Solche Ergänzungen sind mögliche spätere Produktentscheidungen, keine Voraussetzung für den hier erfassten Web-Gleichstand.

## Umsetzung in sinnvoller Reihenfolge

1. **Technische Basis und Gestaltung:** aktuellen Android-Build prüfen, API-Ziel konfigurierbar machen, Gastmodus und Sitzungshandhabung, Navigation, gemeinsame Orts-/Datumszustände, Typografie und wiederverwendbare Bildkarten.
2. **Konzerte vollständig erkunden:** Liste mit Nachladen und Filtern, Konzertdetails, Band- und Ortsprofile, Tickets/Website/Route, Kalender, Karte, Suche und Entdecken.
3. **Persönliche Nutzung:** Merken/Folgen, vollständiges Mein GigLister, Passwortwiederherstellung und E-Mail-Link-Abläufe, überprüfte Push-Zustellung und Navigation zum Konzert.
4. **Eigene Inhalte pflegen:** Konzertanlage und -bearbeitung, Auswahl bestehender oder Anlage neuer Bands/Orte mit Duplikatvorschlägen, Bild-Upload, Profilpflege, Claim-Antrag und objektbezogene Rechteverwaltung.
5. **Abnahme:** Gast, normaler Nutzer, EDIT und MANAGE getrennt prüfen; verweigerter Standort, fehlendes GPS, schlechte Verbindung, abgelaufene Sitzung, leere Listen, fehlende Bilder, lange Line-ups, abgesagte Konzerte, große Ergebnismengen, Light/Dark und große Systemschrift abdecken. Keine Admin-Oberflächen in Android.

Die Reihenfolge priorisiert nutzbare Zwischenstände; alle vier Funktionsschritte gehören zum vollständigen Zielumfang. Native Kotlin/Jetpack Compose kann als vorhandene Basis weiterverwendet werden. Eine neue Auswahl von Karten- oder Bildbibliotheken ist vor der Implementierung anhand aktueller offizieller Dokumentation zu prüfen.

## Zentrale Belegstellen im Repository

- Web-Routen: `frontend/src/app/konzerte/`, `orte/`, `bands/`, `entdecken/`, `suche/`, `mein-giglister/`.
- Web-Design: `frontend/src/app/globals.css`, `frontend/src/app/layout.tsx`, `frontend/src/components/EventCard.tsx`.
- Standort und Zeitraum: `frontend/src/components/LocationPicker.tsx`, `frontend/src/lib/geocode.ts`, `frontend/src/lib/date-range.ts`, `frontend/src/components/ConcertMap.tsx`.
- Formulare und Rechte: `frontend/src/components/EventForm.tsx`, `BandForm.tsx`, `LocationForm.tsx`, `PermissionsPanel.tsx`, `frontend/src/lib/permissions.ts`.
- Backend: `src/main/java/com/giglister/web/` und `src/main/java/com/giglister/service/`, insbesondere EventService und PermissionService.
- Android: `android/app/src/main/java/com/giglister/app/ui/Navigation.kt`, `ui/events/`, `ui/profile/`, `ui/theme/`, `data/api/GigListerApi.kt`, `data/model/`, `push/`, `android/app/build.gradle.kts`.

Live-Abgleich: https://sandbox.fotosvorju.de/konzerte, https://sandbox.fotosvorju.de/konzerte/38, https://sandbox.fotosvorju.de/entdecken.
