# Kids Mode

Kids Mode verwaltet die Kinder und schützt den TV. Das Theme Kids World zeigt nur an.

- **Profile** (`KidsProfile`, `KidsProfiles`): Name, Avatar-Farbe, Lieblingswelt, freigegebene Apps und Favoriten,
  lokal auf dem TV. Es gibt immer mindestens ein Profil. Ältere Freigaben landen im ersten Profil.
- **Elternbereich** (`ParentActivity`, hinter der PIN): Kinder anlegen und löschen, Farbe wechseln, Apps pro Kind
  (gesperrt, frei, Favorit), ruhige Animationen, Einstellungen öffnen, PIN ändern.
- **PIN** (`KidsLock`, `PinHash`): nur als Hash gespeichert, nach 5 Fehlversuchen 60 Sekunden gesperrt.
- **Wächter** (`KidsGuard`, Bedienungshilfe): schluckt Einstellungs-, Eingangs- und Alle-Apps-Taste und schickt Apps
  zurück, die dem aktiven Kind nicht freigegeben sind.

Grenzen und das Einschalten des Wächters stehen in `Themes/KidsWorld/README.md`.

- **Medienzeit** (`MediaTime`, `KidsGate`, `KidsDb`): Konto je Kind mit Grundzeit, Zusatzzeit und Tagesmaximum. Das
  Maximum gilt immer, auch für Korrekturen von Hand. Gezählt wird, wenn eine zählende App vorne ist und der Bildschirm
  an ist; „kostenlose“ Apps kosten nichts. Ist die Zeit um, startet nur noch das Kostenlose. Eine normale App kann den
  TV nicht systemweit sperren: Ohne Wächter greifen die Zeiten nicht, und ein pausiertes Video zählt weiter.
- **Schulaufgaben:** Eltern legen sie am Handy an, das Kind meldet sie am TV als fertig, die Eltern bestätigen oder
  lehnen ab. Erst dann gibt es Minuten. Eine zweite Buchung derselben Aufgabe ist in der Datenbank ausgeschlossen.
  Alle Buchungen stehen im Protokoll.
- **Uhr:** Der Tag läuft nie rückwärts. Verstellte Uhren erscheinen im Protokoll. Vorgestellt bringt die Uhr trotzdem
  einen neuen Tag; dagegen schützen die gesperrten Einstellungen.
- **Sperre:** Solange gesperrt ist oder die Zeit um ist, wirkt auf der Startseite nur noch OK halten (Elternbereich),
  Lautstärke und Ein/Aus. Mit Wächter gilt das auch für Home und Zurück.
- **Nachrichten:** Vom Handy gesendeter Text erscheint groß über allem, auch über laufenden Apps. OK oder Zurück
  schließt ihn. Das braucht den Wächter.
- **Handy-Steuerung** (`KidsRemote`): Zeiten frei einstellen und löschen, Kinder und Einstellungen verwalten. Der TV zeigt im Elternbereich eine Adresse, die man im Handy-Browser im selben
  WLAN öffnet (Port 8686). Anmeldung mit der Eltern-PIN, ein neues Handy wird einmalig mit einem Code vom TV gekoppelt. Sie läuft nur mit eingeschaltetem Wächter. Die Verbindung ist
  unverschlüsselt, die PIN wäre also im eigenen WLAN mitlesbar.

Daten liegen lokal in der App. Beim Überinstallieren eines anderen Themes bleiben sie erhalten, beim Deinstallieren
gehen sie verloren.
