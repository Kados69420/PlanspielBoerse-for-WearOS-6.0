# Planspiel Börse für Wear OS 6

Kleine Uhren-App (Jetpack Compose for Wear OS) für das Sparkassen-Börsenspiel.
Meldet sich direkt mit deinem Planspiel-Börse-Konto an (kein Handy nötig).

## Funktionen
1. Depot ansehen (Gesamtwert, Performance, verfügbares Geld, Rang, Positionen)
2. Verkaufen (Bestens)
3. Aktien kaufen (nur Aktien): aus der Watchlist, per Suche oder Nachkaufen aus dem Depot (Bestens)
4. Stop-Loss einrichten (Verkauf, Order-Typ STOP, Gültigkeit 1/7/14 Tage) und offene Orders löschen
5. Rangliste (mein Rang + Top 10)
6. Kursverlauf (Tag / Woche) pro Aktie, erreichbar in der Position und in der Ordermaske

Design: Dark Mode (schwarz) mit den Farben der Original-App.

## Bauen
1. Android Studio Meerkat (2024.3.2) oder neuer öffnen, Ordner `PlanspielWear` wählen, Sync abwarten.
2. Uhr per WLAN-Debugging koppeln (Entwickleroptionen > ADB-Debugging > Über WLAN debuggen).
3. Run-Konfiguration `app` auf die Uhr starten, oder `./gradlew assembleDebug` und die APK
   (`app/build/outputs/apk/debug/app-debug.apk`) per `adb install` installieren.

Voraussetzung: Wear OS 3+ (minSdk 30), gebaut gegen API 36 (Wear OS 6).

## Hinweise
- Die App nutzt die nicht öffentliche Schnittstelle der Original-App (v4.12.0). Ändert der Betreiber
  die API, müssen die Pfade in `data/Api.kt` angepasst werden. Nur für den eigenen Account/Eigengebrauch.
- Gespeichert werden nur die Tokens (App-privat), nicht das Passwort.
- Farben, Hintergrundverlauf und Schrift stammen aus der Original-App (Rot #EE0000, Grün #04B728,
  Sparkasse-Schrift). Die Schriftdateien sind urheberrechtlich geschützt: nicht weitergeben.
- Der Aufbau (Pakete): `data/` (Modelle + API), `ui/` (Theme, Komponenten, Bildschirme, ViewModel).
