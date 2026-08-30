/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.latin.xlm;

import java.util.Locale;
import java.util.Set;

/**
 * Contextual French homograph disambiguation for unaccented ASCII input.
 *
 * <p>On an AZERTY keyboard without diacritics the user types <code>a</code>,
 * <code>ou</code> and <code>la</code> for both the accent-less word and its
 * accented homograph (<code>à</code>, <code>où</code>, <code>là</code>). The
 * transformer LM is trained to notice these in context, but the native beam
 * search (NUM_RESULTS=3, deterministic decode) frequently ranks the bare ASCII
 * form above the accented candidate, so the suggestion strip never surfaces
 * the accented homograph.</p>
 *
 * <p>This is a conservative, French-only rule. Two families of corrections are
 * produced:</p>
 * <ul>
 *   <li><em>Accent homographs</em>: when the word being typed is exactly
 *       {@code a} or {@code ou} (or one of the accent-only pairs {@code grace},
 *       {@code pres}, {@code plait}, {@code cote}, {@code musee}) and the
 *       immediately preceding committed word is one that, in French grammar,
 *       can almost never be followed by the verb {@code a} / the conjunction
 *       {@code ou}, we bump the accented form to the top of the suggestion
 *       list so the keyboard auto-corrects to it on separator.
 *       The decision is deliberately <em>one-directional</em>: the rule only
 *       ever <em>adds</em> an accented candidate; it never modifies or removes
 *       anything. If the preceding word is not matched, the suggestion list is
 *       left untouched and the stock FUTO merge logic applies. This guarantees
 *       the common cases with the verb {@code a} ("il a peur"), the
 *       conjunction {@code ou} ("thé ou café") and the article {@code la}
 *       ("c'est la vie") are never touched.</li>
 *   <li><em>Subject–verb agreement</em>: when the word being typed is a verb
 *       form and the subject pronoun available in the previous words makes the
 *       typed form ungrammatical, the correct inflected form is bumped to the
 *       top ("tu est" → "tu es", "je fait" → "je fais", "je ne veut" →
 *       "je ne veux", "ils travaille" → "ils travaillent", "elle viennent" →
 *       "elle vient"). Only wrong→right remappings exist, so correctly typed
 *       forms are never touched.</li>
 * </ul>
 *
 * <p>{@code la} is intentionally NOT handled: after a copula the article and
 * the adverb are both grammatical ("elle est la meilleure" vs "elle est là"),
 * and disambiguation requires the *following* word, which is not available
 * while composing. Greedy (off-device) evaluation of the LM gets it right in
 * "elle est là" contexts because it sees the whole sentence.</p>
 */
public final class FrenchHomographRules {
    /**
     * The ngram-context sentinel produced by {@code NgramContext} for the first
     * word of a sentence/field (matching {@code NgramContext.BEGINNING_OF_SENTENCE_TAG}).
     */
    private static final String BEGINNING_OF_SENTENCE_TAG = "<S>";

    private FrenchHomographRules() {
    }

    /**
     * Words after which the preposition {@code à} is grammatically forced when
     * the user types {@code a}: motion/destination verbs, verbs of purpose
     * ("penser à", "servir à", "aider à"), and location verbs.
     *
     * <p>The French verb {@code a} (avoir) is already handled implicitly: it is
     * preceded by a subject ("il", "elle", "on", "Maman", ...) and none of
     * those words appear in this set, so "il a peur" / "Maman a dit" are left
     * unaccented.</p>
     */
    private static final Set<String> PREPOSITION_A_AFTER = Set.of(
            // motion / destination
            "vais", "va", "vas", "allons", "allez", "ira", "irai", "iras", "irons", "irez",
            "aller", "venu", "viens", "vient", "venir", "part", "pars", "partez",
            "rentre", "rentres", "retourne", "retournes", "arrive", "arrives", "arriver",
            "reviens", "revient", "monte", "montes", "descends", "descend",
            // location / living
            "habite", "habites", "habitez", "installe", "installé",
            // purpose / attachment
            "pense", "penses", "pensez", "reve", "reves", "rêve", "rêves", "rever", "rêver",
            "cherche", "cherches", "aide", "aides", "aidez", "sert", "servir", "tient",
            "tenir", "apprend", "apprends", "apprendre", "continue", "continues",
            "commence", "commences", "travaille", "travailles", "travaillez",
            "visite", "visites",
            // copulas ("il est à…", "je suis à…")
            "est", "sont", "suis", "es", "sommes", "etes", "etais", "etait", "étaient",
            "sera", "seront", "serai", "seras",
            // preposition-like heads
            "pres", "près", "proche", "grace", "grâce", "jusqu", "prêt", "prete",
            "prête", "prets", "prêts");

    /**
     * Words after which the unaccented {@code cote} is the noun {@code côté}
     * ("à côté de", "de côté"). Other "cote" meanings ("la cote", stock
     * quotation, "en côte") fall outside this set and stay untouched.
     */
    private static final Set<String> COTE_SIDE_AFTER = Set.of("a", "à", "de");

    private static final Set<String> OU_WHERE_AFTER = Set.of(
            // location / living
            "habite", "habites", "habitez", "vis", "vit", "vivent", "demeure", "demeures",
            "dort", "dors", "dormez", "travaille", "travailles", "travaillez", "nage",
            "nages", "marche", "marches", "dine", "dines", "dîne", "dînes",
            // copulas / state ("tu es où", "il est où")
            "est", "sont", "suis", "es", "sommes", "etes", "etais", "etait", "étaient",
            // movement
            "va", "vais", "vas", "allez", "arrive", "arrives", "part", "pars", "rentre",
            "retourne");

    /** Second-person singular subject pronoun. */
    private static final Set<String> SUBJECT_2SG = Set.of("tu");

    /** First-person singular subject pronouns (possibly elided, "j'"). */
    private static final Set<String> SUBJECT_1SG = Set.of("je", "j");

    /** Third-person plural subject pronouns. */
    private static final Set<String> SUBJECT_3PL = Set.of("ils", "elles");

    /** Third-person singular subject pronouns. */
    private static final Set<String> SUBJECT_3SG = Set.of("il", "elle", "on");

    /** Copulas after which a typed {@code ferme} is the participle "fermé". */
    private static final Set<String> COPULA_FORMS = Set.of(
            "est", "es", "suis", "sommes", "etes", "etait", "etais", "etions", "etiez",
            "étaient", "etant", "été", "sera", "seras", "seront", "serions", "seriez");

    /** Plural être auxiliaries after which a typed {@code arrives} is "arrivés". */
    private static final Set<String> ETRE_PLURAL_AFTER = Set.of(
            "sommes", "etes", "sont", "etions", "etiez", "serons", "serez", "seront");

    /**
     * Avoir auxiliaries after which a typed -er infinitive is the past
     * participle. Includes the elided 1sg "j'ai" which normalizes to "jai" and
     * the concatenated "jai" the AZERTY touch can produce.
     */
    private static final Set<String> AVOIR_AUX_AFTER = Set.of(
            "jai", "ai", "as", "a", "avons", "avez", "ont", "avaient");

    /**
     * Heads (location verbs, à, copulas) after which a typed {@code paris} is
     * the capitalised city name rather than the plural of the noun "pari".
     */
    private static final Set<String> PARIS_CONTEXT_AFTER = Set.of(
            "a", "à", "habite", "habites", "habitez", "est", "suis", "es", "sont",
            "va", "vais", "vas", "aller", "vis", "vit", "vivent", "rentre", "retourne");

    /**
     * Plural determiners after which the typed word is almost always a plural
     * noun ("les voiture" → "les voitures"). Object-pronoun uses ("je les
     * vois") are guarded by {@link #PLURAL_NOUN_VERB_BLACKLIST} and the
     * regular-plural ending checks in {@link #applyPluralAgreement}.
     */
    private static final Set<String> PLURAL_DETERMINERS = Set.of(
            "les", "des", "ces", "mes", "tes", "ses", "nos", "vos", "leurs",
            "deux", "trois", "quatre", "cinq", "six", "sept",
            "plusieurs", "quelques", "aux", "certains", "certaines");

    /**
     * Frequent 1sg/3sg verb forms that must never be given a plural -s after a
     * plural determiner / plural noun ("je les mange", "elle les regarde").
     * Forms already ending in s/x/z (-re verbs: prends, vois, mets, …) or in
     * -ent/-ant (3pl: travaille<b>nt</b>) are excluded by shape in
     * {@link #applyPluralAgreement} and do not need listing here.
     */
    private static final Set<String> PLURAL_NOUN_VERB_BLACKLIST = Set.of(
            "mange", "mangea", "regarde", "donne", "aime", "parle", "parla", "joue",
            "chante", "chantera", "cherche", "trouve", "travaille", "pense", "ecoute",
            "écoute", "demande", "porte", "achete", "achète", "passe", "reste", "tombe",
            "rentre", "monte", "marche", "nage", "dine", "dîne", "dessine", "compte",
            "explique", "raconte", "utilise", "appelle", "remercie", "rencontre", "retrouve",
            "salue", "sert", "dort", "part", "parta", "sort", "court", "sent", "ment",
            "choisit", "finit", "grandit", "ouvre", "ferme", "attend", "met", "dit", "lit",
            "voit", "croit", "nait", "connait", "sait", "veut", "peut", "doit", "va",
            "prend", "comprend", "reprend", "apprend", "vend", "rend", "offre", "suit",
            "boit", "cuit", "construit", "mord", "rompt", "bat");

    /**
     * Adjectives allowed to take a regular plural -s when they follow a plural
     * noun ("les voitures rouge" → "les voitures rouges"). Restricting this to
     * an allowlist keeps the rule free of verb false positives (-er verb stems
     * are not adjectives).
     */
    private static final Set<String> PLURAL_ADJECTIVES = Set.of(
            "rouge", "bleu", "vert", "jaune", "blanc", "noir", "gris", "violet",
            "rose", "orange", "grand", "petit", "joli", "beau", "vieux", "gros",
            "bon", "mauvais", "plein", "haut", "bas", "long", "court");

    /**
     * Common names that end in -ent/-ant but still take a regular plural -s
     * ("les enfant" → "les enfants", "les restaurant" → "les restaurants").
     * Without this the generic -ent/-ant exclusion would leave these common
     * learner words unpluralised.
     */
    private static final Set<String> PLURAL_NOUN_EXCEPTIONS = Set.of(
            "enfant", "restaurant", "appartement", "etablissement", "établissement",
            "gouvernement", "moment", "client", "agent", "ciment", "piment");

    /** Endings that never take a regular plural -s (invariants, 3pl verbs…). */
    private static final String[] NO_REGULAR_PLURAL_ENDINGS = {
            "s", "x", "z", "ent", "ant", "ais", "ois", "is", "al", "au", "eu", "ou"
    };

    /**
     * Feminines determiners after which a following masculine adjective is
     * almost certainly wrong ("une maison bleu" → "bleue"). "une" and "la"
     * (article) are the reliable triggers; object-pronoun uses ("je la vois")
     * are followed by a verb, which is never in {@link #FEMININE_ADJECTIVES}.
     */
    private static final Set<String> FEMININE_TRIGGERS = Set.of(
            "une", "la", "ma", "ta", "sa", "cette");

    /**
     * Masculine adjective → feminine form for the common FLE colour/size
     * adjectives. Words already ending in "e" are feminine on purpose and
     * never touched by the rule.
     */
    private static final Set<String> FEMININE_ADJECTIVES_ENABLED = Set.of(
            "bleu", "grand", "petit", "vert", "gris", "violet", "joli", "gros",
            "bon", "mauvais", "blanc", "long", "haut", "bas", "noir", "vieux",
            "beau", "chaud", "froid", "doux", "franc", "gentil", "pliant", "poli");

    private static String feminineForm(final String adjective) {
        switch (adjective) {
            case "blanc": return "blanche";
            case "long": return "longue";
            case "bas": return "basse";
            case "vieux": return "vieille";
            case "beau": return "belle";
            case "doux": return "douce";
            case "franc": return "franche";
            case "gentil": return "gentille";
            case "violet": return "violette";
            case "gros": return "grosse";
            case "bon": return "bonne";
            case "mauvais": return "mauvaise";
            default: return adjective + "e";
        }
    }

    /**
     * Returns the corrected (accented / properly inflected) homograph that
     * should replace {@code typedWord}, or {@code null} when there is nothing
     * to correct for this context.
     *
     * @param typedWord the word currently being composed (may be capitalized)
     * @param prevWords the committed words before {@code typedWord}, most
     *                  recent last; empty when the word is the first of the
     *                  field. The last element is the {@code NgramContext}
     *                  sentence-start sentinel {@code <S>} at sentence start.
     */
    public static String apply(final String typedWord, final String[] prevWords) {
        if (typedWord == null || typedWord.isEmpty()) {
            return null;
        }
        final String word = typedWord.toLowerCase(Locale.ROOT);
        final boolean atStart = isSentenceStart(prevWords);
        final String prevWord = prevWords.length == 0 ? "" : prevWords[prevWords.length - 1];
        switch (word) {
            case "a":
                if (atStart) {
                    // Sentence-initial "a" is almost always the preposition
                    // ("À cause de…"); the verb form "a" needs a subject and
                    // can only appear after one. Inject the capital so the
                    // capitalised "A" from the LM beam does not win.
                    return "À";
                }
                if (isOneOf(prevWord, SUBJECT_2SG)) {
                    // "tu a" -> "tu as": the 2sg of avoir is "as".
                    return "as";
                }
                if (isOneOf(prevWord, PREPOSITION_A_AFTER)) {
                    return "à";
                }
                return null;
            case "ou":
                if (isOneOf(prevWord, OU_WHERE_AFTER)) {
                    return "où";
                }
                return null;
            case "est":
                // "tu est" / "tu es" through the same oral form: after the
                // 2sg pronoun only "es" is grammatical.
                if (isOneOf(prevWord, SUBJECT_2SG)) {
                    return "es";
                }
                return null;
            case "veut":
                // "je veut" / "je ne veut": the 1sg present of vouloir is
                // "veux"; a 3sg "veut" needs il/elle/on.
                if (withinLastTwo(prevWords, SUBJECT_1SG)) {
                    return "veux";
                }
                return null;
            case "fait":
                // "je fait": the 1sg present of faire is "fais".
                if (withinLastTwo(prevWords, SUBJECT_1SG)) {
                    return "fais";
                }
                return null;
            case "travaille":
                // "ils travaille" -> "ils travaillent": the 3pl ending is -ent.
                if (isOneOf(prevWord, SUBJECT_3PL)) {
                    return "travaillent";
                }
                return null;
            case "chante":
                if (isOneOf(prevWord, SUBJECT_3PL)) {
                    return "chantent";
                }
                return null;
            case "joue":
                if (isOneOf(prevWord, SUBJECT_3PL)) {
                    return "jouent";
                }
                return null;
            case "vient":
                // "ils vient" -> "ils viennent"; "tu vient" -> "tu viens".
                if (isOneOf(prevWord, SUBJECT_3PL)) {
                    return "viennent";
                }
                if (isOneOf(prevWord, SUBJECT_2SG)) {
                    return "viens";
                }
                return null;
            case "viennent":
                // Reverse agreement: after a 3sg subject the ending is -t.
                if (isOneOf(prevWord, SUBJECT_3SG)) {
                    return "vient";
                }
                return null;
            case "arrives":
                // "nous sommes arrives" -> "nous sommes arrivés": plural
                // agreement of the past participle.
                if (isOneOf(prevWord, ETRE_PLURAL_AFTER)) {
                    return "arrivés";
                }
                return null;
            case "viens":
                // "il faut que tu viens" -> "viens" -> "viennes": the present
                // subjunctive after the impersonal "il faut que".
                if (containsInLastN(prevWords, 2, "que") && containsInLastN(prevWords, 3, "faut")) {
                    return "viennes";
                }
                return null;
            case "manger":
                // "j'ai manger" / "nous avons manger" / "elle a manger": after
                // an avoir auxiliary the -er infinitive is the past participle
                // ("mangé"). A bare "manger" (infinitive after vouloir/aller)
                // is not preceded by these auxiliaries and stays untouched.
                if (isOneOf(prevWord, AVOIR_AUX_AFTER)) {
                    return "mangé";
                }
                return null;
            case "gateaux":
                // No accent-less French word "gateaux"; "gâteaux" is the only use.
                return atStart ? "Gâteaux" : "gâteaux";
            case "paris":
                // The city name is always capitalised ("je vais à Paris").
                // Unaccented "paris" (plural of "pari", a bet) follows concrete
                // subjects ("des paris", "ses paris") which are not these
                // location/copula heads.
                if (isOneOf(prevWord, PARIS_CONTEXT_AFTER)) {
                    return "Paris";
                }
                return null;
            case "grace":
                // No accent-less French word "grace"; only the proper name
                // (capitalised "Grace") is legitimate. Return the accented
                // form, capitalised at sentence start.
                return atStart ? "Grâce" : "grâce";
            case "pres":
                // No accent-less French word "pres"; "près" is the only use.
                return atStart ? "Près" : "près";
            case "plait":
                // Only the circumflexed spelling is standard ("s'il vous plaît").
                return atStart ? "Plaît" : "plaît";
            case "musee":
                // No accent-less French noun "musee"; "musée" is the only use.
                return atStart ? "Musée" : "musée";
            case "ferme":
                // After a copula a typed "ferme" is the past participle
                // ("la porte est ferme" -> "fermé"). The verb "fermer" and the
                // adjective/noun "ferme" appear with concrete subjects, which
                // are not copulas.
                if (isOneOf(prevWord, COPULA_FORMS)) {
                    return "fermé";
                }
                return null;
            case "javais":
                // Concatenated elision ("javais" for "j'avais") has no
                // legitimate accent-less French word: the only reading is the
                // 1sg imperfect of avoir.
                return atStart ? "J'avais" : "j'avais";
            case "cote":
                if (isOneOf(prevWord, COTE_SIDE_AFTER)) {
                    return "côté";
                }
                return null;
            default:
                final String feminine = applyFeminineAgreement(word, prevWords);
                if (feminine != null) {
                    return feminine;
                }
                return applyPluralAgreement(word, prevWord);
        }
    }

    /**
     * Fixes the gender of a colour/size adjective written in the masculine
     * after a feminine determiner ("une maison bleu" -> "bleue",
     * "la petite fille grand" -> "grande"). A trigger determiner such as "la"
     * is also a direct object; we only risk the change when the following
     * word is one of the allow-listed adjectives, and a direct object is
     * always followed by a verb, which never sits in
     * {@link #FEMININE_ADJECTIVES_ENABLED}.
     */
    /**
     * When the adjective directly follows one of these prepositions it belongs
     * to a nominal phrase ("une photo en noir et blanc", "une maison de bois
     * blanc") where the masculine is correct and the feminine determiner in
     * the context does not govern it.
     */
    private static final Set<String> GENDER_PHRASE_BARRIERS = Set.of(
            "en", "de", "du", "au", "aux", "à", "par", "pour", "avec",
            "sans", "dans");

    private static String applyFeminineAgreement(final String word,
            final String[] prevWords) {
        if (!FEMININE_ADJECTIVES_ENABLED.contains(word) || word.endsWith("e")) {
            return null;
        }
        if (!containsTriggerInLastThree(prevWords, FEMININE_TRIGGERS)) {
            return null;
        }
        if (prevWords != null && prevWords.length > 0
                && isOneOf(prevWords[prevWords.length - 1], GENDER_PHRASE_BARRIERS)) {
            return null;
        }
        return feminineForm(word);
    }

    private static boolean containsTriggerInLastThree(final String[] prevWords,
            final Set<String> triggers) {
        if (prevWords == null || prevWords.length == 0) {
            return false;
        }
        for (int i = prevWords.length - 1;
                i >= Math.max(0, prevWords.length - 3); i--) {
            if (isOneOf(prevWords[i], triggers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the regular plural -s to a noun after a plural determiner ("les
     * voiture" → "voitures", "des fleur" → "fleurs") and to a colour/size
     * adjective after a plural noun ("les voitures rouge" → "rouges").
     * Grammatical verbs are never touched: -er stems sit in
     * {@link #PLURAL_NOUN_VERB_BLACKLIST}, 3pl/participles are caught by the
     * -ent/-ant endings, -re/-ir present forms already end in s/x/z, and the
     * adjective rule is an allowlist.
     */
    private static String applyPluralAgreement(final String word, final String prevWord) {
        if (word.length() < 2 || isOneOf(word, PLURAL_NOUN_VERB_BLACKLIST)) {
            return null;
        }
        final String prev = prevWord == null ? "" : normalize(prevWord);
        if (prev.isEmpty()) {
            return null;
        }
        final boolean afterPluralDeterminer = PLURAL_DETERMINERS.contains(prev);
        final boolean afterPluralNoun = prev.endsWith("s") && !PLURAL_DETERMINERS.contains(prev);
        if (!afterPluralDeterminer && !afterPluralNoun) {
            return null;
        }
        for (final String ending : NO_REGULAR_PLURAL_ENDINGS) {
            if (word.endsWith(ending) && !PLURAL_NOUN_EXCEPTIONS.contains(word)) {
                return null;
            }
        }
        if (afterPluralNoun && !PLURAL_ADJECTIVES.contains(word)) {
            return null;
        }
        return word + "s";
    }

    /**
     * Convenience overload for callers that only track the single previous
     * word. Accepts {@code null} (no previous word).
     */
    public static String apply(final String typedWord, final String prevWord) {
        return apply(typedWord, prevWord == null ? new String[0] : new String[] { prevWord });
    }

    private static boolean isSentenceStart(final String[] prevWords) {
        if (prevWords == null || prevWords.length == 0) {
            return true;
        }
        final String last = prevWords[prevWords.length - 1];
        return last == null || last.isEmpty() || BEGINNING_OF_SENTENCE_TAG.equals(last);
    }

    /**
     * Returns true when {@code set} contains any of the two most recent
     * previous words, normalizing apostrophes and capitals. Used for
     * "je ne veut" where the subject is one word before the negation.
     */
    private static boolean withinLastTwo(final String[] prevWords, final Set<String> set) {
        if (prevWords == null || prevWords.length == 0) {
            return false;
        }
        final int n = prevWords.length;
        return isOneOf(prevWords[n - 1], set)
                || (n >= 2 && isOneOf(prevWords[n - 2], set));
    }

    /**
     * Returns true when {@code word} matches (normalized) any of the
     * {@code n} most recent previous words, most recent first.
     */
    private static boolean containsInLastN(final String[] prevWords, final int n, final String word) {
        if (prevWords == null || prevWords.length == 0) {
            return false;
        }
        for (int i = prevWords.length - 1; i >= Math.max(0, prevWords.length - n); i--) {
            if (equalsNormalized(prevWords[i], word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOneOf(final String word, final Set<String> candidates) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        return candidates.contains(normalize(word));
    }

    private static boolean equalsNormalized(final String word, final String candidate) {
        return word != null && normalize(word).equals(candidate);
    }

    private static String normalize(final String word) {
        return word.toLowerCase(Locale.ROOT)
                .replace("'", "").replace("’", "").replace(".", "")
                .replace(",", "").replace("-", " ").trim();
    }
}