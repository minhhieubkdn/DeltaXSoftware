package cc.imwi.deltaxsoftware;

public class PositionData {
    public int x = 0;
    public int y = 0;
    public int z = 0;
    public int w = 0;

    public PositionData(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public PositionData(int x, int y, int z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PositionData(int x, int y, int z, int w){
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

}
