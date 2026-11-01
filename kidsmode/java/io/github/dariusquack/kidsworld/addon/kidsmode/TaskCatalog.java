package io.github.dariusquack.kidsworld.addon.kidsmode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vorlagen für Aufgaben. Eine Übernahme erzeugt je Kind eine eigene Aufgabe; die Vorlagen selbst ändern sich nie.
 * Altersgruppen sind nur Empfehlungen. Reine Daten, testbar.
 */
public final class TaskCatalog {
    public static final int SCHOOL = 0, ROOM = 1, HOME = 2, SPECIAL = 3;
    /** Altersgruppen als Bits: 5 bis 8, 9 bis 11, 12 bis 14 Jahre. */
    public static final int AGE_YOUNG = 1, AGE_MIDDLE = 2, AGE_OLD = 4, AGE_ALL = 7;
    private static final String[] SYMBOLS = {"📚", "🧹", "🏠", "💡"};

    /** Eine Vorlage. */
    public static final class Entry {
        public final int category, ages, minutes;
        public final String german, english;

        Entry(int category, int ages, int minutes, String german, String english) {
            this.category = category;
            this.ages = ages;
            this.minutes = minutes;
            this.german = german;
            this.english = english;
        }

        public String title(String language) {
            return KidsLanguage.GERMAN.equals(language) ? german : english;
        }
    }

    private static final int M = AGE_MIDDLE | AGE_OLD, Y = AGE_YOUNG | AGE_MIDDLE, A = AGE_ALL;
    private static final List<Entry> ALL = Collections.unmodifiableList(build());

    private TaskCatalog() {}

    public static List<Entry> all() {
        return ALL;
    }

    public static String symbol(int category) {
        return SYMBOLS[Math.floorMod(category, SYMBOLS.length)];
    }

    /** Vorgeschlagene Altersgruppe als Bit; 0 wenn das Alter fehlt oder nicht passt. */
    public static int ageGroup(int age) {
        if (age >= 5 && age <= 8) return AGE_YOUNG;
        if (age >= 9 && age <= 11) return AGE_MIDDLE;
        if (age >= 12 && age <= 14) return AGE_OLD;
        return 0;
    }

    private static List<Entry> build() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(SCHOOL, A, 15, "Hausaufgaben erledigen", "Do your homework"));
        e.add(new Entry(SCHOOL, A, 10, "10 Minuten lesen", "Read for 10 minutes"));
        e.add(new Entry(SCHOOL, M, 10, "Vokabeln lernen", "Learn vocabulary"));
        e.add(new Entry(SCHOOL, M, 15, "Für einen Test lernen", "Study for a test"));
        e.add(new Entry(SCHOOL, A, 5, "Ein Arbeitsblatt bearbeiten", "Finish a worksheet"));
        e.add(new Entry(SCHOOL, Y, 10, "Einmaleins üben", "Practise times tables"));
        e.add(new Entry(SCHOOL, Y, 10, "Schreibübungen machen", "Do writing practice"));
        e.add(new Entry(SCHOOL, M, 10, "Schulstoff wiederholen", "Review school work"));
        e.add(new Entry(SCHOOL, A, 5, "Schulranzen vollständig packen", "Pack your school bag"));
        e.add(new Entry(SCHOOL, M, 5, "Schulmaterialien sortieren", "Sort your school things"));
        e.add(new Entry(ROOM, A, 15, "Eigenes Zimmer aufräumen", "Tidy your room"));
        e.add(new Entry(ROOM, A, 5, "Bett machen", "Make your bed"));
        e.add(new Entry(ROOM, Y, 5, "Spielzeug wegräumen", "Put your toys away"));
        e.add(new Entry(ROOM, A, 5, "Schreibtisch aufräumen", "Tidy your desk"));
        e.add(new Entry(ROOM, A, 5, "Schmutzwäsche in den Wäschekorb legen", "Put dirty clothes in the basket"));
        e.add(new Entry(ROOM, M, 10, "Saubere Wäsche zusammenlegen", "Fold clean laundry"));
        e.add(new Entry(ROOM, M, 10, "Staubsaugen im eigenen Zimmer", "Vacuum your room"));
        e.add(new Entry(ROOM, M, 5, "Papierkorb leeren", "Empty the wastebasket"));
        e.add(new Entry(ROOM, A, 10, "Bücher und Spiele sortieren", "Sort books and games"));
        e.add(new Entry(ROOM, A, 5, "Kleidung für den nächsten Tag bereitlegen", "Lay out clothes for tomorrow"));
        e.add(new Entry(HOME, A, 5, "Tisch decken", "Set the table"));
        e.add(new Entry(HOME, A, 5, "Tisch abräumen", "Clear the table"));
        e.add(new Entry(HOME, M, 10, "Spülmaschine ausräumen", "Empty the dishwasher"));
        e.add(new Entry(HOME, M, 10, "Spülmaschine einräumen", "Load the dishwasher"));
        e.add(new Entry(HOME, Y, 5, "Besteck sortieren", "Sort the cutlery"));
        e.add(new Entry(HOME, A, 10, "Einkäufe mit wegräumen", "Help put the shopping away"));
        e.add(new Entry(HOME, M, 5, "Müll wegbringen", "Take out the rubbish"));
        e.add(new Entry(HOME, A, 10, "Beim Kochen helfen", "Help with cooking"));
        e.add(new Entry(HOME, Y, 5, "Getränke für alle bereitstellen", "Get drinks for everyone"));
        e.add(new Entry(HOME, A, 10, "Gemeinschaftsbereich aufräumen", "Tidy a shared room"));
        e.add(new Entry(HOME, A, 10, "Beim Wäsche sortieren helfen", "Help sort the laundry"));
        e.add(new Entry(HOME, A, 10, "Geschwister beim Aufräumen unterstützen", "Help a sibling tidy up"));
        e.add(new Entry(SPECIAL, A, 5, "Pflanzen gießen", "Water the plants"));
        e.add(new Entry(SPECIAL, A, 5, "Beim Autoausladen helfen", "Help unload the car"));
        e.add(new Entry(SPECIAL, A, 5, "Schuhe ordentlich aufstellen", "Line up the shoes"));
        e.add(new Entry(SPECIAL, A, 5, "Garderobe aufräumen", "Tidy the coat rack"));
        e.add(new Entry(SPECIAL, A, 10, "Spielbereich gemeinsam aufräumen", "Tidy the play area together"));
        e.add(new Entry(SPECIAL, M, 15, "Eine größere Haushaltsaufgabe übernehmen", "Take on a bigger household job"));
        e.add(new Entry(SPECIAL, M, 10, "Beim Putzen einer Fläche helfen", "Help clean a surface"));
        // Spanne 5 bis 15 Minuten: Startwert 10, Eltern stellen ihn beim Übernehmen ein.
        e.add(new Entry(SPECIAL, A, 10, "Eine selbst gewählte Familienaufgabe erledigen", "Do a family job of your choice"));
        return e;
    }
}
