package constants;

/**
 * contains the possible values of a "rank" for an edge
 * @author benjamin linard
 */
public class Rank {

    public static final int NO_RANK=0;
    public static final int SUPERKINGDOM=1;
    public static final int KINGDOM=2;
    public static final int SUBKINGDOM=3;
    public static final int SUPERPHYLUM=4;
    public static final int PHYLUM=5;
    public static final int SUBPHYLUM=6;
    public static final int SUPERCLASS=7;
    public static final int CLASS=8;
    public static final int SUBCLASS=9;
    public static final int INFRACLASS=10;
    public static final int COHORT=11;
    public static final int SUBCOHORT=12;
    public static final int SUPERORDER=13;
    public static final int ORDER=14;
    public static final int SUBORDER=15;
    public static final int INFRAORDER=16;
    public static final int PARVORDER=17;
    public static final int SECTION=18;
    public static final int SUBSECTION=19;
    public static final int SUPERFAMILY=20;
    public static final int FAMILY=21;
    public static final int SUBFAMILY=22;
    public static final int TRIBE=23;
    public static final int SUBTRIBE=24;
    public static final int GENUS=25;
    public static final int SUBGENUS=26;
    public static final int SERIES=27;
    public static final int SPECIES_GROUP=28;
    public static final int SPECIES_SUBGROUP=29;
    public static final int SPECIES=30;
    public static final int SUBSPECIES=31;
    public static final int VARIETAS=32;
    public static final int FORMA=33;

    //order MUST match the list of previous static fields
    private static final String[] ranks={
            "no rank",
            "superkingdom",
            "kingdom",
            "subkingdom",
            "superphylum",
            "phylum",
            "subphylum",
            "superclass",
            "class",
            "subclass",
            "infraclass",
            "cohort",
            "subcohort",
            "superorder",
            "order",
            "suborder",
            "infraorder",
            "parvorder",
            "section",
            "subsection",
            "superfamily",
            "family",
            "subfamily",
            "tribe",
            "subtribe",
            "genus",
            "subgenus",
            "series",
            "species group",
            "species subgroup",
            "species",
            "subspecies",
            "varietas",
            "forma"
    };

    /**
     * check if the string is a known rank (ignores case)
     * @param word
     * @return
     */
    public static boolean isRank(String word) {
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i].equalsIgnoreCase(word))
                return true;
        }
        return false;
    }

    /**
     * get the id of a rank, return -1 if unknown
     * @param word
     * @return
     */
    public static int getRank(String word) {
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i].equalsIgnoreCase(word))
                return i;
        }
        return -1;
    }

    /**
     * get the string representation of a rank_id
     * @param rank_id
     * @return
     */
    public static String getRankString(int rank_id) {
        return ranks[rank_id];
    }

    /**
     * get the list of available ranks
     * @return
     */
    public static String[] getRanks() {
        return ranks;
    }
}
