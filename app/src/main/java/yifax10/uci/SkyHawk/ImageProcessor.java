package yifax10.uci.SkyHawk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public final class ImageProcessor {
    private static int cannyThreshold = 20; //20
    private static int lineThreshold = 30;  //30
    private static double minLength = 55;   //55
    private static double gap = 25;         //25
    private static double buffer = 55;      //55
    private static double WIDTH = 57;       //57,54
    private static double whiteValue = 600;    //600,450

    private static double clus_dist = 60;   //40


    public static Mat process(Mat image){
        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();

        //Size size0 = image.size();
        //image = image.submat(110, (int)size0.height, 210, (int)size0.width-210);
        //rotate(image,11.3);
        // convert to grayscale
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // reduce noise with a 3x3 kernel
        Imgproc.blur(grayImage, detectedEdges, new Size(5, 5));

        // canny detector, with ratio of lower:upper threshold of 4:1
        Imgproc.Canny(detectedEdges, detectedEdges, cannyThreshold, cannyThreshold*4);

        // Copy edges to the images that will display the results in BGR
        Mat cdst=new Mat();
        Imgproc.cvtColor(detectedEdges, cdst, Imgproc.COLOR_GRAY2BGR);
        Mat cdstP = cdst.clone();

        // Standard Hough Line Transform
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(detectedEdges, linesP, 1, Math.PI/180, lineThreshold, minLength, gap); // runs the actual detection

        // Draw the lines (lineP = a clean list of lines)
        ArrayList<double[]> hlines = new ArrayList<>();
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            if (Math.abs(l[3]-l[1]) <=10) {
                Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
                hlines.add(l);
            }
        }

        Collections.sort(hlines, new Comparator<double[]>() {
            @Override
            public int compare(double[] d1, double[] d2) {
                if (d1[0] == d2[0]) {
                    return 0;
                } else {
                    return d1[0] < d2[0]? -1 : 1;
                }
            }
        });


        // step 3: find clusters of x1 close together - clust_dist apart
        ArrayList<ArrayList<double[]>> clusters = new ArrayList<>();
        int dIndex = 0;
        for (int i = 0; i < hlines.size()-1; i++) {
            double distance = Math.abs(hlines.get(i+1)[0]-hlines.get(i)[0]);

            if (distance <= clus_dist) {
                if (clusters.size() <= dIndex) {
                    ArrayList<double[]> temp = new ArrayList<>();
                    clusters.add(temp);
                    clusters.get(dIndex).add(hlines.get(i));
                    clusters.get(dIndex).add(hlines.get(i+1));
                } else {
                    clusters.get(dIndex).add(hlines.get(i));
                    clusters.get(dIndex).add(hlines.get(i+1));
                }
            } else {
                dIndex += 1;
            }
        }


        //step 4: identify coordinates of rectangle around this cluster
        ArrayList<double[]> rects = new ArrayList<>();
        double[] width = new double[clusters.size()];
        double avg_width=0;
        int count = 0;
        for (int i = 0; i < clusters.size(); i++) {
            count=0;
            ArrayList<double[]> all = clusters.get(i);
            if (all.size() > 1) {
                Collections.sort(all, new Comparator<double[]>() {
                    @Override
                    public int compare(double[] d1, double[] d2) {
                        if (d1[1] == d2[1]) {
                            return 0;
                        } else {
                            return d1[1] < d2[1]? -1 : 1;
                        }
                    }
                });

                double avg_y1 = all.get(0)[1];
                double avg_y2 = all.get(all.size()-1)[1];
                //System.out.println("avg y1, avg y2:\n"+avg_y1+"\n" + avg_y2+"\n");
                double avg_x1 = 0;
                double avg_x2 = 0;
                for (int j = 0; j < all.size(); j++) {
                    avg_x1 += all.get(j)[0];
                    avg_x2 += all.get(j)[2];
                }
                for (int k = 5; k < all.size()-3; k++) {
                    if (all.get(k)[1]-all.get(k-1)[1]>30) {
                        width[i] = width[i] + all.get(k)[1] - all.get(k-1)[1];
                        System.out.println(width[i]);
                        count++;
                    }
                }
                width[i] = width[i]/count;
                System.out.println("width[i] is"+width[i]+"\n");



                avg_x1 = avg_x1/all.size();
                avg_x2 = avg_x2/all.size();
                double[] rect = {avg_x1,avg_y1,avg_x2, avg_y2};
                rects.add(rect);
            }
        }
        for (int i=0; i<width.length;i++) {
            avg_width += width[i];
        }
        setWIDTH(avg_width/width.length + 9);
        System.out.println("final width is" + avg_width/width.length+"\n");

        System.out.println("Num of parking lanes: "+ rects.size()+"\n");
        Size size = image.size();


        Collection<Spot> spots = new ArrayList<>();
        int total = 0;
        int available = 0;
        for (int i = 0; i < rects.size(); i++) {
            Point topLeft = new Point(rects.get(i)[0] - buffer,rects.get(i)[1]);
            Point botRight = new Point(rects.get(i)[2] + buffer,rects.get(i)[3]);

            for (int j = 0; j < Math.abs(botRight.y - topLeft.y) / WIDTH -1; j++){
                Point p1 = new Point(topLeft.x, topLeft.y + j*WIDTH);
                Point p2 = new Point(topLeft.x + (botRight.x - topLeft.x)/2 - 5, topLeft.y + (j+1)*WIDTH);
                Spot spot1 = new Spot(p1,p2);

                Point p3 = new Point(topLeft.x + (botRight.x - topLeft.x)/2 - 5, topLeft.y + j*WIDTH);
                Point p4 = new Point(botRight.x , topLeft.y + (j+1)*WIDTH);
                Spot spot2 = new Spot(p3,p4);

                spots.add(spot1);
                spots.add(spot2);
                total+=2;
                Imgproc.rectangle(image, p1, p2, new Scalar(0,0,255),2);
                Imgproc.rectangle(image, p3, p4, new Scalar(0,0,255),2);
            }
            Imgproc.rectangle(image, topLeft, botRight, new Scalar(255,0,0),3);

        }
        System.out.println("total number of parking spot: " + total +"\n");

        int white =0;
        for (Spot s: spots) {
            white = 0;
            s.setAvailable(true);
            System.out.println((int)s.getLeftTop().x +"," + (int)s.getRightBottom().x + "\n");
            System.out.println((int)s.getLeftTop().y +"," + (int)s.getRightBottom().y + "\n");
            double[] rgb = new double[3];
            for (int i = (int) s.getLeftTop().x;i<(int) s.getRightBottom().x&&i<size.width; i ++) {
                for (int j = (int)s.getLeftTop().y ; j<(int) s.getRightBottom().y&&j<size.height ;j ++) {
                    if (cdst==null) {
                        System.out.println("cdst null");
                    }
                    if(cdst.get(j, i)==null) {
                        System.out.println("get null");

                    }
                    rgb[0] = cdst.get(j, i)[0];
                    rgb[1] = cdst.get(j, i)[1];
                    rgb[2] = cdst.get(j, i)[2];

                    //System.out.println("rgb is" + rgb[0]+","+rgb[1]+","+rgb[2]+"\n");
                    if (rgb[0]>0 || rgb[1]>0 || rgb[2]>0) {
                        white+=1;

                    }
                }
            }
            System.out.println("white is :" + white + "\n");
            if (white<=whiteValue) {
                s.setAvailable(true);
                System.out.println("set true\n");
                available+=1;
            }else {
                System.out.println("occupied");
                s.setAvailable(false);
            }
        }
        System.out.println("total number of available: " + available);

        Mat blank = new Mat(image.rows(), image.cols(), image.type());
        for (Spot s: spots) {
            if(s.isAvailable()==true) {
                Imgproc.rectangle(blank, s.getLeftTop(), s.getRightBottom(), new Scalar(0,255,0),-1);
            }
        }
        Core.addWeighted(image, 1.0, blank, 0.3, 0, image);

        Mat dest = new Mat();
        image.copyTo(dest, image);

        return dest;
    }

    public static void rotate(Mat image, double angle) {
        //Calculate size of new matrix
        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) (image.width() * cos + image.height() * sin);
        int newHeight = (int) (image.width() * sin + image.height() * cos);

        // rotating image
        Point center = new Point(newWidth / 2, newHeight / 2);
        Mat rotMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0); //1.0 means 100 % scale

        Size size = new Size(newWidth, newHeight);
        Imgproc.warpAffine(image, image, rotMatrix, image.size());
    }

    public static int getCannyThreshold() {
        return cannyThreshold;
    }

    public static void setCannyThreshold(int cannyThreshold) {
        ImageProcessor.cannyThreshold = cannyThreshold;
    }

    public static int getLineThreshold() {
        return lineThreshold;
    }

    public static void setLineThreshold(int lineThreshold) {
        ImageProcessor.lineThreshold = lineThreshold;
    }

    public static double getMinLength() {
        return minLength;
    }

    public static void setMinLength(double minLength) {
        ImageProcessor.minLength = minLength;
    }

    public static double getGap() {
        return gap;
    }

    public static void setGap(double gap) {
        ImageProcessor.gap = gap;
    }



    public static double getWIDTH() {
        return WIDTH;
    }



    public static void setWIDTH(double wIDTH) {
        WIDTH = wIDTH;
    }
}
