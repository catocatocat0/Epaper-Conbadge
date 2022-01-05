package catocatocato.epaper.conbadge.image_processing;

import android.graphics.Bitmap;

import catocatocato.epaper.conbadge.MainActivity;

public class EPaperPicture{

    private static final int[][] palette =
            {
                    {0, 0, 0},
                    {255, 255, 255},
                    {0, 255, 0},
                    {0, 0, 255},
                    {255, 0, 0},
                    {255, 255, 0},
                    {255, 128, 0},
                    {255, 225, 200} //Peach
                    //{255, 162, 255} //Lilac
                    //{35, 45, 20} //Dark Green
            };

    //---------------------------------------------------------
    //Creates an image using the EPaper screen's color palette
    public static Bitmap createIndexedImage()
    {
        // Bitmap of source image
        Bitmap srcBmp = MainActivity.originalImage;
        // Bitmap of destination image
        Bitmap dstBmp = srcBmp.copy(srcBmp.getConfig(), true);

        //applies the dithering algorithm
        int w = dstBmp.getWidth();
        int h = dstBmp.getHeight();

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                //select the current and new RGB
                int imgRGB = dstBmp.getPixel(x, y);
                int newRGB = findClosestColor(imgRGB);
                dstBmp.setPixel(x, y, newRGB);

                //converts the hex color values into RGB values
                int[] imgRGBPalette = separateRGBA(imgRGB);
                int[] newRGBPalette = separateRGBA(newRGB);

                //calculates the errors
                int errR = imgRGBPalette[0] - newRGBPalette[0];
                int errG = imgRGBPalette[1] - newRGBPalette[1];
                int errB = imgRGBPalette[2] - newRGBPalette[2];

                //Burke's Dithering
                if (x + 1 < w){
                    int update = calculateColor(dstBmp.getPixel(x + 1, y), errR, errG, errB, 8, 32F);
                    dstBmp.setPixel(x + 1, y, update);
                }
                if (x + 2 < w){
                    int update = calculateColor(dstBmp.getPixel(x + 2, y), errR, errG, errB, 4, 32F);
                    dstBmp.setPixel(x + 2, y, update);
                }
                if (y + 1 < h) {
                    int update = calculateColor(dstBmp.getPixel(x , y + 1), errR, errG, errB, 8, 32F);
                    dstBmp.setPixel(x, y + 1, update);
                }
                if (x - 1 >= 0 && y + 1 < h) {
                    int update = calculateColor(dstBmp.getPixel(x - 1, y + 1), errR, errG, errB, 4, 32F);
                    dstBmp.setPixel(x - 1, y + 1, update);
                }
                if (x - 2 >= 0 && y + 1 < h) {
                    int update = calculateColor(dstBmp.getPixel(x - 2, y + 1), errR, errG, errB, 2, 32F);
                    dstBmp.setPixel(x - 2, y + 1, update);
                }
                if (y + 1 < h && x + 1 < w) {
                    int update = calculateColor(dstBmp.getPixel(x + 1, y + 1), errR, errG, errB, 4, 32F);
                    dstBmp.setPixel(x + 1, y + 1, update);
                }
                if (y + 1 < h && x + 2 < w) {
                    int update = calculateColor(dstBmp.getPixel(x + 2, y + 1), errR, errG, errB, 2, 32F);
                    dstBmp.setPixel(x + 2, y + 1, update);
                }
            }
        }

        return dstBmp;
    }

    public static int calculateColor(int selRGB, int errR, int errG, int errB, int dither, float base){
        int[] selRGBArray = separateRGBA(selRGB);

        selRGBArray[0] += errR * (dither / base);
        selRGBArray[1] += errG * (dither / base);
        selRGBArray[2] += errB * (dither / base);

        if (selRGBArray[0] < 0) {
            selRGBArray[0] = 0;
        } else if (selRGBArray[0] > 255) {
            selRGBArray[0] = 255;
        }
        if (selRGBArray[1] < 0) {
            selRGBArray[1] = 0;
        } else if (selRGBArray[1] > 255) {
            selRGBArray[1] = 255;
        }
        if (selRGBArray[2] < 0) {
            selRGBArray[2] = 0;
        } else if (selRGBArray[2] > 255) {
            selRGBArray[2] = 255;
        }

        return combineRGBA(selRGBArray);
    }

    public static int findClosestColor(int rgbHex){
        int[][] rgbPalette = palette;
        int[] rgbSelector = separateRGBA(rgbHex);
        int[] closestColor = new int[3];
        double bestColorDistance = Integer.MAX_VALUE;

        //calculates the closest color
        for(int i = 0; i < rgbPalette.length; i++){

            int r = rgbPalette[i][0] - rgbSelector[0];
            int g = rgbPalette[i][1] - rgbSelector[1];
            int b = rgbPalette[i][2] - rgbSelector[2];

            double colorDistance =
                    Math.pow(r,2) +
                            Math.pow(g,2) +
                            Math.pow(b,2);
            //this model has been adjusted to 'normalize' the colors
            double adjBlack = 1.25;
            double adjWhite = 1;
            double adjRed = 1.15;
            double adjGreen = 0.95;
            double adjBlue = 1;
            double adjYellow = 1;
            double adjOrange = 1.66;
            double adjLilac = 3;
            switch (i) {
                case 0: //black
                    colorDistance = colorDistance * adjBlack;
                    break;
                case 1: //white
                    colorDistance = colorDistance * adjWhite;
                    break;
                case 2: //red
                    colorDistance = colorDistance * adjRed;
                    break;
                case 3: //green
                    colorDistance = colorDistance * adjGreen;
                    break;
                case 4: //blue
                    colorDistance = colorDistance * adjBlue;
                    break;
                case 5: //Yellow
                    colorDistance = colorDistance * adjYellow;
                    break;
                case 6: //Orange
                    colorDistance = colorDistance * adjOrange;
                    break;
                case 7: //Lilac
                    colorDistance = colorDistance * adjLilac;
                    break;
            }

            if (colorDistance < bestColorDistance) {
                bestColorDistance = colorDistance;
                closestColor = rgbPalette[i];
            }
        }
        return combineRGBA(closestColor);
    }

    //converts an RGB int[] into a 32bit RGB integer
    //returns a 32bit RGB integer
    public static int combineRGBA(int[] RGB){
        int r = (RGB[0] << 16) & 0x00FF0000;
        int g = (RGB[1] << 8) & 0x0000FF00;
        int b = RGB[2] & 0x000000FF;

        return 0xFF000000 | r | g | b;
    }

    //converts 32bit RGBA into R, G and B components as an int[3], index are ordered RGB
    //RGBA - the 32bit RGBA integer
    //return an int[] with separate RGB values
    public static int[] separateRGBA(int RGBA){
        int[] colorChannels = new int[4];

        //red
        colorChannels[0] = (RGBA >> 16) & 0xFF;
        //green
        colorChannels[1] = (RGBA >> 8) & 0xFF;
        //blue
        colorChannels[2] = RGBA & 0xFF;

        return colorChannels;
    }
}