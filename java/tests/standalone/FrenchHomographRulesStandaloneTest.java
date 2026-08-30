/*
 * Standalone test for FrenchHomographRules.
 *
 * Compile & run from the android-keyboard repo root:
 *   javac -d /tmp/frhr $(find java/tests/standalone java/src/org/futo/inputmethod/latin/xlm -name "FrenchHomographRules*.java")
 *   java -cp /tmp/frhr org.futo.inputmethod.latin.tests.standalone.FrenchHomographRulesStandaloneTest
 */

package org.futo.inputmethod.latin.tests.standalone;

import java.util.Locale;

import org.futo.inputmethod.latin.xlm.FrenchHomographRules;

public final class FrenchHomographRulesStandaloneTest {
    private static int total = 0;
    private static int failed = 0;

    private static void check(final String name, final String actual, final String expected) {
        total++;
        if ((actual == null && expected == null) || (expected != null && expected.equals(actual))) {
            System.out.println("PASS " + name + " -> " + actual);
        } else {
            failed++;
            System.out.println("FAIL " + name + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    public static void main(final String[] args) {
        // ---- a -> à : positive contexts ----
        check("je vais a", FrenchHomographRules.apply("a", "vais"), "à");
        check("il va a", FrenchHomographRules.apply("a", "va"), "à");
        check("tu vas a", FrenchHomographRules.apply("a", "vas"), "à");
        check("aller a", FrenchHomographRules.apply("a", "aller"), "à");
        check("il habite a", FrenchHomographRules.apply("a", "habite"), "à");
        check("je pense a", FrenchHomographRules.apply("a", "pense"), "à");
        check("merci pour tout, grace a toi", FrenchHomographRules.apply("a", "grace"), "à");
        check("pres a", FrenchHomographRules.apply("a", "pres"), "à");
        check("il est a", FrenchHomographRules.apply("a", "est"), "à");
        check("je suis a", FrenchHomographRules.apply("a", "suis"), "à");
        check("sera a", FrenchHomographRules.apply("a", "sera"), "à");
        check("jusqu a", FrenchHomographRules.apply("a", "jusqu"), "à");
        check("prêt a", FrenchHomographRules.apply("a", "prêt"), "à");
        check("capitalized A", FrenchHomographRules.apply("A", "vais"), "à");

        // ---- a -> a : negative contexts (verb avoir etc.) ----
        check("il a peur", FrenchHomographRules.apply("a", "il"), null);
        check("elle a vu", FrenchHomographRules.apply("a", "elle"), null);
        check("on a dit", FrenchHomographRules.apply("a", "on"), null);
        check("Maman a dit", FrenchHomographRules.apply("a", "Maman"), null);
        check("qui a", FrenchHomographRules.apply("a", "qui"), null);
        check("que a", FrenchHomographRules.apply("a", "que"), null);
        check("y a", FrenchHomographRules.apply("a", "y"), null);
        check("no context", FrenchHomographRules.apply("a", "the"), null);
        check("sentence start (empty prev)", FrenchHomographRules.apply("a", ""), "À");
        check("sentence start sentinel <S>", FrenchHomographRules.apply("a", "<S>"), "À");
        check("sentence start lowercase sentinel", FrenchHomographRules.apply("a", "<s>"), null);

        // ---- grace -> grâce : no accent-less counterpart ----
        check("merci, grace a toi", FrenchHomographRules.apply("grace", "merci"), "grâce");
        check("grace sentence start", FrenchHomographRules.apply("grace", ""), "Grâce");
        check("grace sentinel <S>", FrenchHomographRules.apply("grace", "<S>"), "Grâce");

        // ---- pres -> près ----
        check("pres de moi", FrenchHomographRules.apply("pres", "sommes"), "près");
        check("pres sentence start", FrenchHomographRules.apply("pres", ""), "Près");
        check("pres sentinel <S>", FrenchHomographRules.apply("pres", "<S>"), "Près");

        // ---- plait -> plaît ----
        check("s'il vous plait", FrenchHomographRules.apply("plait", "vous"), "plaît");
        check("plait sentence start", FrenchHomographRules.apply("plait", ""), "Plaît");
        check("plait sentinel <S>", FrenchHomographRules.apply("plait", "<S>"), "Plaît");

        // ---- cote -> côté : only after a/à/de ----
        check("a cote (prev a)", FrenchHomographRules.apply("cote", "a"), "côté");
        check("à cote (prev à)", FrenchHomographRules.apply("cote", "à"), "côté");
        check("de cote (prev de)", FrenchHomographRules.apply("cote", "de"), "côté");
        check("la cote (prev la)", FrenchHomographRules.apply("cote", "la"), null);
        check("en cote (prev en)", FrenchHomographRules.apply("cote", "en"), null);
        check("cote no context", FrenchHomographRules.apply("cote", ""), null);

        // ---- ou -> où : positive contexts ----
        check("il habite ou", FrenchHomographRules.apply("ou", "habite"), "où");
        check("elle vit ou", FrenchHomographRules.apply("ou", "vit"), "où");
        check("tu es ou", FrenchHomographRules.apply("ou", "es"), "où");
        check("il est ou", FrenchHomographRules.apply("ou", "est"), "où");
        check("va ou", FrenchHomographRules.apply("ou", "va"), "où");
        check("il dort ou", FrenchHomographRules.apply("ou", "dort"), "où");
        check("tu vais ou", FrenchHomographRules.apply("ou", "vais"), "où");

        // ---- ou -> ou : negative contexts (conjunction) ----
        check("the ou cafe", FrenchHomographRules.apply("ou", "the"), null);
        check("non ou", FrenchHomographRules.apply("ou", "non"), null);
        check("soit ou", FrenchHomographRules.apply("ou", "soit"), null);
        check("no context", FrenchHomographRules.apply("ou", ""), null);

        // ---- la : intentionally unsupported ----
        check("elle est la -> NOT là", FrenchHomographRules.apply("la", "est"), null);
        check("c'est la vie", FrenchHomographRules.apply("la", "c'est"), null);

        // ---- other typed words untouched ----
        check("bonjour intact", FrenchHomographRules.apply("bonjour", "vais"), null);
        check("empty", FrenchHomographRules.apply("", "vais"), null);
        check("null", FrenchHomographRules.apply(null, "vais"), null);

        // ---- punctuation normalization of prev word ----
        check("prev with dot", FrenchHomographRules.apply("a", "vais..."), "à");

        // ---- subject-verb agreement ----
        check("tu est -> es", FrenchHomographRules.apply("est", "tu"), "es");
        check("tu a -> as", FrenchHomographRules.apply("a", "tu"), "as");
        check("je veut -> veux", FrenchHomographRules.apply("veut", "je"), "veux");
        check("je ne veut -> veux (array)",
                FrenchHomographRules.apply("veut", new String[] { "je", "ne" }), "veux");
        check("il veut stays", FrenchHomographRules.apply("veut", "il"), null);
        check("je fait -> fais", FrenchHomographRules.apply("fait", "je"), "fais");
        check("ils fait stays? (no je)", FrenchHomographRules.apply("fait", "ils"), null);
        check("ils travaille -> travaillent", FrenchHomographRules.apply("travaille", "ils"), "travaillent");
        check("elles chante -> chantent", FrenchHomographRules.apply("chante", "elles"), "chantent");
        check("elles joue -> jouent", FrenchHomographRules.apply("joue", "elles"), "jouent");
        check("elle joue stays", FrenchHomographRules.apply("joue", "elle"), null);
        check("ils vient -> viennent", FrenchHomographRules.apply("vient", "ils"), "viennent");
        check("tu vient -> viens", FrenchHomographRules.apply("vient", "tu"), "viens");
        check("il vient stays", FrenchHomographRules.apply("vient", "il"), null);
        check("elle viennent -> vient", FrenchHomographRules.apply("viennent", "elle"), "vient");
        check("ils viennent stays", FrenchHomographRules.apply("viennent", "ils"), null);
        check("nous sommes arrives -> arrivés",
                FrenchHomographRules.apply("arrives", "sommes"), "arrivés");
        check("elles sont arrives -> arrivés",
                FrenchHomographRules.apply("arrives", "sont"), "arrivés");
        check("tu arrives stays", FrenchHomographRules.apply("arrives", "tu"), null);

        // ---- accent-only nouns ----
        check("le musee -> le musée", FrenchHomographRules.apply("musee", "le"), "musée");
        check("musee sentence start", FrenchHomographRules.apply("musee", ""), "Musée");
        check("la porte est ferme -> fermé", FrenchHomographRules.apply("ferme", "est"), "fermé");
        check("elle ne ferme pas (verb stays)", FrenchHomographRules.apply("ferme", "ne"), null);
        check("la ferme stays", FrenchHomographRules.apply("ferme", "la"), null);

        // ---- past participles after avoir ----
        check("j'ai manger -> mangé", FrenchHomographRules.apply("manger", "J'ai"), "mangé");
        check("nous avons manger -> mangé", FrenchHomographRules.apply("manger", "avons"), "mangé");
        check("elle a manger -> mangé", FrenchHomographRules.apply("manger", "a"), "mangé");
        check("je vais manger (stays infinitive)", FrenchHomographRules.apply("manger", "vais"), null);
        check("je veux manger (stays infinitive)", FrenchHomographRules.apply("manger", "veux"), null);

        // ---- gâteaux : no accent-less counterpart ----
        check("des gateaux -> gâteaux", FrenchHomographRules.apply("gateaux", "des"), "gâteaux");

        // ---- Paris : capital city after location head ----
        check("il habite a paris -> à Paris", FrenchHomographRules.apply("paris", "à"), "Paris");
        check("elle est a paris -> à Paris", FrenchHomographRules.apply("paris", "à"), "Paris");
        check("je vais a paris", FrenchHomographRules.apply("paris", "a"), "Paris");
        check("des paris (bets) stays", FrenchHomographRules.apply("paris", "des"), null);

        // ---- subjunctive after "il faut que" ----
        check("il faut que tu viens -> viennes",
                FrenchHomographRules.apply("viens", new String[] { "il", "faut", "que", "tu" }), "viennes");
        check("tu viens (no faut) stays", FrenchHomographRules.apply("viens", "tu"), null);

        // ---- concatenated elision : javais -> j'avais ----
        check("javais -> j'avais", FrenchHomographRules.apply("javais", "si"), "j'avais");
        check("javais sentence start -> J'avais",
                FrenchHomographRules.apply("javais", ""), "J'avais");
        check("javais sentinel <S> -> J'avais",
                FrenchHomographRules.apply("javais", "<S>"), "J'avais");

        // ---- plural nouns after plural determiners ----
        check("les voiture -> voitures", FrenchHomographRules.apply("voiture", "les"), "voitures");
        check("des fleur -> fleurs", FrenchHomographRules.apply("fleur", "des"), "fleurs");
        check("ces enfant -> enfants", FrenchHomographRules.apply("enfant", "ces"), "enfants");
        check("les grand -> grands", FrenchHomographRules.apply("grand", "les"), "grands");
        check("mes table -> tables", FrenchHomographRules.apply("table", "mes"), "tables");
        check("les chat -> chats", FrenchHomographRules.apply("chat", "les"), "chats");
        check("deux livre -> livres", FrenchHomographRules.apply("livre", "deux"), "livres");
        check("plusieurs maison -> maisons",
                FrenchHomographRules.apply("maison", "plusieurs"), "maisons");
        // already plural stays
        check("les voitures stays", FrenchHomographRules.apply("voitures", "les"), null);
        check("les enfants stays", FrenchHomographRules.apply("enfants", "des"), null);
        // irregular / invariants untouched
        check("les appartement -> appartements",
                FrenchHomographRules.apply("appartement", "les"), "appartements");
        check("les etablissement -> etablissements",
                FrenchHomographRules.apply("etablissement", "les"), "etablissements");
        check("les cheval (irregular, untouched)", FrenchHomographRules.apply("cheval", "les"), null);
        check("les Francais (invariant, untouched)", FrenchHomographRules.apply("francais", "les"), null);
        check("les gouvernement (common noun) -> gouvernements",
                FrenchHomographRules.apply("gouvernement", "les"), "gouvernements");
        check("les restaurant -> restaurants", FrenchHomographRules.apply("restaurant", "les"), "restaurants");

        // ---- verb after plural determiner never pluralized ----
        check("je les mange stays", FrenchHomographRules.apply("mange", "les"), null);
        check("il les regarde stays", FrenchHomographRules.apply("regarde", "les"), null);
        check("tu les vois stays", FrenchHomographRules.apply("vois", "les"), null);
        check("il les met stays", FrenchHomographRules.apply("met", "les"), null);
        check("elle les voit stays", FrenchHomographRules.apply("voit", "les"), null);
        check("ils les mangent stays", FrenchHomographRules.apply("mangent", "les"), null);
        check("il passe souvent stays", FrenchHomographRules.apply("passe", "il"), null);

        // ---- adjectives after plural nouns (allowlist) ----
        check("les voitures rouge -> rouges",
                FrenchHomographRules.apply("rouge", "voitures"), "rouges");
        check("des maisons grand -> grands",
                FrenchHomographRules.apply("grand", "maisons"), "grands");
        check("les fleurs jaune -> jaunes",
                FrenchHomographRules.apply("jaune", "fleurs"), "jaunes");
        check("les enfants jouent (3pl verb untouched)",
                FrenchHomographRules.apply("jouent", "enfants"), null);
        check("les voitures roulent (3pl verb untouched)",
                FrenchHomographRules.apply("roulent", "voitures"), null);
        check("le matin (after 2sg verb stays)",
                FrenchHomographRules.apply("le", "travailles"), null);
        check("samedi (after 2sg verb stays)",
                FrenchHomographRules.apply("samedi", "travailles"), null);

        // ---- feminine agreement after a feminine determiner ----
        check("une maison bleu -> bleue",
                FrenchHomographRules.apply("bleu", new String[] { "une", "maison" }), "bleue");
        check("la petite fille grand -> grande",
                FrenchHomographRules.apply("grand", new String[] { "la", "petite", "fille" }),
                "grande");
        check("une robe blanc -> blanche",
                FrenchHomographRules.apply("blanc", new String[] { "une", "robe" }), "blanche");
        check("cette chambre vieux -> vieille",
                FrenchHomographRules.apply("vieux", new String[] { "cette", "chambre" }),
                "vieille");
        check("sa voix doux -> douce",
                FrenchHomographRules.apply("doux", new String[] { "sa", "voix" }), "douce");
        check("ta table long -> longue",
                FrenchHomographRules.apply("long", new String[] { "ta", "table" }), "longue");
        check("une classe beau -> belle",
                FrenchHomographRules.apply("beau", new String[] { "une", "classe" }), "belle");
        check("ma valise gros -> grosse",
                FrenchHomographRules.apply("gros", new String[] { "ma", "valise" }), "grosse");
        check("mon père bleu stays (mon not trigger)",
                FrenchHomographRules.apply("bleu", new String[] { "mon", "père" }), null);
        check("il a un manteau bleu stays (un not trigger)",
                FrenchHomographRules.apply("bleu", new String[] { "il", "a", "manteau" }), null);
        check("les voitures rouge plural stays (no feminine trigger)",
                FrenchHomographRules.apply("rouge", "voitures"), "rouges");
        check("elle est la meilleure stays (la + verb, not adjective)",
                FrenchHomographRules.apply("meilleure", new String[] { "elle", "est", "la" }),
                null);
        check("une photo en noir et blanc stays (set phrase)",
                FrenchHomographRules.apply("noir", new String[] { "une", "photo", "en" }),
                null);
        check("petite already feminine stays",
                FrenchHomographRules.apply("petite", new String[] { "une", "fille" }), null);
        check("bonjour intact after une (not an adjective)",
                FrenchHomographRules.apply("bonjour", new String[] { "une", "chose" }), null);

        System.out.println();
        System.out.println(failed + "/" + total + " tests failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}