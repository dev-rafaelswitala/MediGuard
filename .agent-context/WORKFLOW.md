# Git-Workflow

```
main          ← nur bei Versions-Releases (z. B. 2.0, 2.1)
  └── dev     ← Integrationsbranch für laufende Arbeit
        └── feature/<kurzname>  ← konkrete Features/Fixes
```

## Ablauf
1. Stand auf `main` oder `dev` committen („Snapshot“)
2. `dev` von aktuellem Stand abzweigen (falls nicht vorhanden)
3. Für Arbeit: `feature/...` von `dev`
4. Regelmäßige kleine Commits auf dem Feature-Branch
5. Fertig → merge in `dev` (lokal)
6. Release → merge `dev` → `main`, Tag optional, APK bauen
7. **Kein Push**, bis der User es sagt

## Commit-Stil
- Kurz, warum-fokussiert
- Deutsch oder Englisch ok, konsistent halten
- Keine APKs, keine Secrets
