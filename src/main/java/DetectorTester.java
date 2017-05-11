import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve_000 on 2017-05-01.
 */
public class DetectorTester {
    final static private String CASCADE = "cascades/birdheadcascadev6.xml";
    final static private String root = "CascadeTraining/cascade_res";
    final static String targetFolder = root + CASCADE.substring(9,CASCADE.length()-4) ;
    final static String sourceFolder = "birdsExamples";

    static int nr = 0;

    DetectorTester(String foldername){
        File folder = new File(foldername);
        List<String> names = getImageNames(folder);
        names.sort((a,b) -> a.compareTo(b));
        System.out.println(names);
        run(names.toArray(new String[]{}));
    }

    public void run(String[] str){

        // Load classifier and image
        CascadeClassifier detector = new CascadeClassifier();
        System.out.println("Classifier loaded=" + detector.load(CASCADE));

        int s = str.length, a = 0;
        Mat[] bird = new Mat[s];
        MatOfRect[] bd = new MatOfRect[s];
        for(int i = 0; i < s; i++) {
            bird[i] = Imgcodecs.imread(str[i]);
            bd[i] = new MatOfRect();
            detector.detectMultiScale(bird[i], bd[i]);
            System.out.println(String.format("Detected %s tits for file=%s", bd[i].toArray().length,str[i]));
            a += bd[i].toArray().length;
        }

        File trgtFldr = new File(targetFolder + nr);
        if(a > 0 && !trgtFldr.exists())
            trgtFldr.mkdir();
        else{

            while(trgtFldr.exists()){
                trgtFldr = new File(targetFolder+ ++nr);
            }
            trgtFldr.mkdir();
        }


        for(int i = 0; i < s; i++) {
            if(bd[i].toArray().length < 1)
                continue;
            for (Rect rect : bd[i].toArray()) {
                Imgproc.rectangle(bird[i], new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
            }
            String filename = targetFolder + nr + "/birdDetected" + i + ".jpg";
            System.out.println(String.format("Writing %s", filename));
            Imgcodecs.imwrite(filename, bird[i]);
        }

        // Save the visualized detection.


    }

    public List<String> getImageNames(final File folder) {
        List<String> files = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                continue;
            } else {
                files.add(folder + "/" + fileEntry.getName());
            }
        }
        return files;
    }


    public static void main(String[] args){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        DetectorTester d = new DetectorTester(sourceFolder);
    }

}
