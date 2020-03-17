package graph;

/**
 * simple edge represention
 * @author benjamin linard
 */
public class TaxoEdge {

    private int rank=-1;
    private int division=-1;

    public TaxoEdge(int rank, int division) {
        this.rank=rank;
        this.division=division;
    }

    public int getRank() {
        return rank;
    }

    public int getDivision() {
        return division;
    }

}
