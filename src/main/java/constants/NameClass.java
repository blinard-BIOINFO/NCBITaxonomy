package constants;

/**
 * contains the possible values of a "name_class" for a node "name"
 * @author benjamin linard
 */
public class NameClass {

    public static final int ACRONYM=0;
    public static final int ANAMORPH=1;
    public static final int AUTHORITY=2;
    public static final int BLAST_NAME=3;
    public static final int COMMON_NAME=4;
    public static final int EQUIVALENT_NAME=5;
    public static final int GENBANK_ACRONYM=6;
    public static final int GENBANK_ANAMORPH=7;
    public static final int GENEBANK_COMMON_NAME=8;
    public static final int GENBANK_SYNONYM=9;
    public static final int INCLUDES=10;
    public static final int IN_PART=11;
    public static final int MISNOMER=12;
    public static final int MISSPELLING=13;
    public static final int SCIENTIFIC_NAME=14;
    public static final int SYNONYM=15;
    public static final int TELEOMORPH=16;
    public static final int TYPE_MATERIAL=17;

    private static final String[] nameClasses={  "acronym",
                                    "anamorph",
                                    "authority",
                                    "blast name",
                                    "common name",
                                    "equivalent name",
                                    "genbank acronym",
                                    "genbank anamorph",
                                    "genbank common name",
                                    "genbank synonym",
                                    "includes",
                                    "in-part",
                                    "misnomer",
                                    "misspelling",
                                    "scientific name",
                                    "synonym",
                                    "teleomorph",
                                    "type material"
                                };
    
    /*
     * check is the string is a know name class (ignore case)
     */
    public static boolean isClass(String word) {
        for (int i = 0; i < nameClasses.length; i++) {
            if (nameClasses[i].equalsIgnoreCase(word))
                return true;
        }
        return false;
    }
    /*
     * get the id of a name class, return -1 if unknown
     */
    public static int getClass(String word) {
        for (int i = 0; i < nameClasses.length; i++) {
            if (nameClasses[i].equalsIgnoreCase(word))
                return i;
        }
        return -1;
    }
    /*
     * get the list of available name classes
     */
    public static String[] getClasses() {
        return nameClasses;
    }
    
            
}
