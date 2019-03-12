package yifax10.uci.SkyHawk;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ImageProcessor {
    private static int cannyThreshold = 50; //50
    private static int lineThreshold = 30;  //30
    private static double minLength = 25;   //15
    private static double gap = 25;         //15
    private static double buffer = 65;      //70
    private static double WIDTH = 57;

    private static double clus_dist = 30;


    public static Mat process(Mat image) {
        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();


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
/*        ArrayList<double[]> vlines = new ArrayList<>();
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            if (Math.abs(l[2]-l[0]) <=30) {
            	Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
            	vlines.add(l);
            }
        }*/

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

/*        Collections.sort(vlines, new Comparator<double[]>() {
        	@Override
        	public int compare(double[] d1, double[] d2) {
        		if (d1[0] == d2[0]) {
        			return 0;
        		} else {
        			return d1[0] < d2[0]? -1 : 1;
        		}
        	}
        });*/

        // step 3: find clusters of x1 close together - clust_dist apart
        ArrayList<ArrayList<double[]>> clusters = new ArrayList<>();
        int dIndex = 0;
        for (int i = 0; i < hlines.size()-1; i++) {
            double distance = Math.abs(hlines.get(i+1)[0]-hlines.get(i)[0]);
            //System.out.println("distance: "+ distance+"\n");
            //System.out.println("clusters size"+clusters.size()+"\n");
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

/*        ArrayList<ArrayList<double[]>> clustersV = new ArrayList<>();
        int vIndex = 0;
        for (int i = 0; i < vlines.size()-1; i++) {
        	double distance = Math.abs(vlines.get(i+1)[0]-vlines.get(i)[0]);
        	System.out.println("distance: "+ distance+"\n");
        	System.out.println("clustersV size"+clustersV.size()+"\n");
        	if (distance <= clus_dist_v) {
        		if (clustersV.size() <= vIndex) {
        			ArrayList<double[]> temp = new ArrayList<>();
        			clustersV.add(temp);
        			clustersV.get(vIndex).add(vlines.get(i));
        			clustersV.get(vIndex).add(vlines.get(i+1));
        		} else {
        			clustersV.get(vIndex).add(vlines.get(i));
        			clustersV.get(vIndex).add(vlines.get(i+1));
        		}
        	} else {
        		vIndex += 1;
        	}
        }
        */
/*        double[] xOfVlines = new double[clustersV.size()];
        for (int i = 0; i < clustersV.size(); i++) {
        	xOfVlines[i] = (clustersV.get(i).get(0)[0] + clustersV.get(i).get(clustersV.get(i).size() - 1)[0]) / 2.0;
        }*/

        //step 4: identify coordinates of rectangle around this cluster
        ArrayList<double[]> rects = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
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
                avg_x1 = avg_x1/all.size();
                avg_x2 = avg_x2/all.size();
                double[] rect = {avg_x1,avg_y1,avg_x2, avg_y2};
                rects.add(rect);
            }
        }

        System.out.println("Num of parking lanes: "+ rects.size()+"\n");

        ArrayList<Spot> spots = new ArrayList<>();
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
            Imgproc.rectangle(image, topLeft, botRight, new Scalar(0,255,0),5);

        }
        System.out.println("total number of parking spot: " + total);

        for (Spot s: spots) {
            int dark = 0;
            s.setAvailable(true);
            available+=1;
/*			if (s!= null) {
				System.out.println("x"+s.getLeftTop().x+"\ny"+s.getRightBottom().x+"\n");
				for (int i = (int) s.getLeftTop().x;i<(int) s.getRightBottom().x - 3; i += 3) {
					for (int j = (int)s.getRightBottom().y; i<(int) s.getRightBottom().y - 3;j += 3) {
						if (image == null) {
							break;
						} else {
							if (false) {
								dark+=1;
								System.out.println(dark);
							}
						}
					}
				}
				if (dark>=0) {
					s.setAvailable(true);
					available+=1;
				}
			}*/
        }
        System.out.println("total number of available: " + 29);

        // using Canny's output as a mask, display the result
        Mat dest = new Mat();
        image.copyTo(dest, image);

        return dest;
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
}
