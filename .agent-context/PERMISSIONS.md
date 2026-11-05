# Berechtigungen & Grenzen

## Erlaubt (ohne Rückfrage)
- Code lesen/ändern im lokalen Repo
- Lokale Git-Operationen: commit, branch, merge (dev ← feature)
- Debug-APK bauen und als `MediGuardVx.y.apk` ablegen
- Emulator starten/testen, **wenn** Android SDK/AVD lokal verfügbar
- Docs und `.agent-context/` aktualisieren
- Sinnvolle UX-/Bugfixes und kleine Ergänzungen

## Nur mit ausdrücklicher User-Erlaubnis
- `git push` / Remote ändern
- Force-Push, Hard-Reset, History rewrite
- Release-Keystore / Play Store Upload
- Cloud-Sync, Analytics, externe APIs
- Löschen von Nutzerdaten-Konzepten ohne Spec

## Nicht tun
- Secrets committen
- `MediGuardV*.apk` ins Git (zu groß; nur lokal)
- App als Medizinprodukt / Diagnose-Tool vermarkten
- Mehr als 5 Bottom-Tabs
