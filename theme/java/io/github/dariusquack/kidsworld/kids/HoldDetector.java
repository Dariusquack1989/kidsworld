package io.github.dariusquack.kidsworld.kids;

/**
 * Unterscheidet bei der OK-Taste einen kurzen Druck (App starten) vom langen Halten (Elternzugang).
 * Die Fernbedienung schickt beim Halten wiederholte Tastendrücke; gezählt wird die Zeit seit dem ersten.
 */
public final class HoldDetector {
    public enum Action { NONE, PRESS, HOLD }

    private final long holdMs;
    private long downAt = -1;
    private boolean fired;

    public HoldDetector(long holdMs) {
        this.holdMs = holdMs;
    }

    /** Taste unten (auch Wiederholung). HOLD genau einmal, sobald lange genug gehalten. */
    public Action down(long downTime, long eventTime, int repeatCount) {
        if (repeatCount == 0 || downAt < 0) {
            downAt = downTime;
            fired = false;
        }
        if (!fired && eventTime - downAt >= holdMs) {
            fired = true;
            return Action.HOLD;
        }
        return Action.NONE;
    }

    /** Taste losgelassen: PRESS, wenn vorher nicht schon HOLD kam. */
    public Action up() {
        boolean wasHold = fired;
        boolean wasDown = downAt >= 0;
        downAt = -1;
        fired = false;
        return wasDown && !wasHold ? Action.PRESS : Action.NONE;
    }
}
