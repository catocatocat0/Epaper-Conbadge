package catocatocato.epaper.conbadge.image_processing;

public class EPaperDisplay
{
    public int    width;
    public int    height;
    public int    index;
    public String title;

    public EPaperDisplay(int width, int height, int index)
    {
        this.width  = width;
        this.height = height;
        this.index  = index;
    }

    //returns the E-Paper Display
    public static EPaperDisplay getDisplay()
    {
        return new EPaperDisplay(600,448,7);
    }
}