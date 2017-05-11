import javafx.util.Pair;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.Buffer;
import java.util.*;
import java.util.List;

/**
 * Created by steve_000 on 2017-05-06.
 */
public class Detector {
    final private CascadeClassifier head_detector;
    final private String CASCADE = "cascades/birdheadcascadev1.xml";
    final private String data = "Talgoxe:Talgoxe (Parus major) är en fågel inom ordningen tättingar och familjen mesar. Den är vanlig i hela Europa, västra, centrala och norra Asien och delar av Nordafrika och förekommer i alla sorters skogslandskap. Den är vanligen stannfågel och de flesta talgoxar flyttar inte förutom under extremt hårda vintrar. Talgoxen är den mest utbredda arten i släktet Parus. Dess taxonomi är komplicerad och omdiskuterad, och den delas upp i en mängd underarter, som i sin tur delas in i tre till fyra underartsgrupper. Vissa auktoriteter behandlar tre av dessa grupper som egna arter, exempelvis turkestanmes." +
            "&Björktrast:Björktrast (Turdus pilaris) är en trast som är vanligt förekommande i sitt häckningsområde i norra Palearktis. Björktrasten är en långstjärtad och kraftigt byggd trast. Dess nacke, övergump, nedre delen av ryggen, huvudets ovansida och kroppssidorna är askgrå. Skuldrorna och övre delen av ryggen är kastanjebruna och de undre vingtäckarna är vita. Buk och undergump är vit och den är kraftigt vattrad med pilspetsformade fläckar på kroppssidorna under vingarna." +
            "&Koltrast:Koltrast (Turdus merula) är en fågel som tillhör familjen trastfåglar. Den räknas som den mest utbredda trastfågeln i Europa. Tidigare var koltrasten en utpräglad skogsfågel men den har sedan mitten av 1800-talet bosatt sig i parker nära bebyggelse samt i trädgårdar vilket i dag gör den till en kulturföljare. Tidigare behandlades en stor mängd trastpopulationer i palearktis såsom tillhörande arten, men genetiska studier indikerar att de bättre beskrivs som fyra arter: koltrast, kinesisk koltrast (T. mandarinus), tibetkoltrast (T. maximus) och indisk koltrast (T. simillimus)." +
            "&Domherre:Domherre (Pyrrhula pyrrhula) är en stor och kraftig fink som förekommer i Europa och Asien, inklusive Kamtjatka och Japan. Den lever främst i barrskog från lågland till bergsskogar, gärna i granplanteringar, men också i gles blandskog med små barrträd och rik undervegetation, eller parker och trädgårdar. Födan består främst av frön och knoppar. Domherren kategoriseras inte som hotad.";
    final private String[] data2 = {"Sverige", "Norge", "Danmark", "Finland"};

    final HashMap<String, String> birds;
    final HashMap<Integer, String> ids;
    final HashMap<String, Set<Integer>> countries;
    final List<Mat[]> histograms;
    final Mat[] refereces;


    Detector(){
        head_detector = new CascadeClassifier();
        System.out.println("Classifier loaded=" + head_detector.load(CASCADE));

        String[] rows = data.split("&");
        Arrays.sort(rows);
        int i = 0;
        HashMap<String,String> tmpBirds = new HashMap<>();
        HashMap<Integer, String> tmpIDs = new HashMap<>();
        HashMap<String, Set<Integer>> tmpCountries = new HashMap<>();
        for(String str: rows){
            String[] bird = str.split(":");
            tmpIDs.put(i, bird[0]);
            tmpBirds.put(bird[0], bird[1]);
            i++;
        }

        Set<Integer> id = tmpIDs.keySet();
        for(String str: data2){
            tmpCountries.put(str, id);
        }

        birds = tmpBirds;
        ids = tmpIDs;
        countries = tmpCountries;

        refereces = getImages("bird_heads");

        histograms = calcHistograms(refereces.clone());

        i = 0;
        Mat[] tmp = histograms.get(3);
        for(Mat[] mat: histograms){

            System.out.println(i++);
            System.out.println("Histogram test" + Imgproc.compareHist(mat[0], tmp[0], MY_METHOD));
            System.out.println("Histogram test" + Imgproc.compareHist(mat[1], tmp[1], MY_METHOD));
            System.out.println();
        }
    }

    public JSONObject analyze(BufferedImage image, String country) {
        Pair<String,Mat> res = scan(image);

        String img = getImage(res.getKey());
        int bird;

        if(country == null || country.equals(""))
            bird = findBird(res.getValue(), null);
        else
            bird = findBird(res.getValue(), countries.get(country));

        return result(img, bird);
    }

    private Scalar[] cols = {new Scalar(0, 255, 0), new Scalar(0, 0, 255), new Scalar(255, 0, 0) , new Scalar(255, 0, 0) , new Scalar(255, 0, 0) , new Scalar(255, 0, 0) };

    private Pair<String,Mat> scan(BufferedImage image){
        String res = null;

        //Get a copy of image, in BGR format
        BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        imageCopy.getGraphics().drawImage(image, 0, 0, null);

        // Convert BufferedImage to MAT
        byte[] img = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();
        Mat source = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        source.put(0,0, img);

        MatOfRect target = new MatOfRect();
        Mat cropped = null;

        head_detector.detectMultiScale(source, target);
        if(target.toArray().length > 0){
            int i = 0;
            for (Rect rect : target.toArray()) {
                cropped = new Mat(source, rect);
                Imgcodecs.imwrite("cropped" + i +".jpg",cropped);
                Imgproc.rectangle(source, new org.opencv.core.Point(rect.x, rect.y), new org.opencv.core.Point(rect.x + rect.width, rect.y + rect.height), cols[i++]);

            }
            res = "result.jpg";
            Imgcodecs.imwrite(res,source);
            cropped = new Mat(source, target.toArray()[0]);
        }

        return new Pair(res, cropped);
    }

    private String getImage(String res){
        if(res == null)
            return null;

        BufferedImage result;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            result = ImageIO.read(new File(res));
            ImageIO.write(result, "jpg", os);

        } catch (IOException e) {
            e.printStackTrace();
        }

        String base64 = Base64.getEncoder().encodeToString(os.toByteArray());
        return base64;
    }

    private JSONObject result(String img, int bird){
        JSONObject res = new JSONObject();

        if(img == null){
            res.put("status","none");
        }else{
            res.put("status","success");
            res.put("image", img);

            if(bird == -1)
                res.put("text", "Couldn't find a match for bird");
            else{
                res.put("text", getBird(bird));
            }
        }
        return res;
    }

    private int findBird(Mat img, Set<Integer> ids){
        if(img == null || img.empty())
            return -1;

        Mat[] hist = calcHistogram(img);
        if(ids == null  || ids.isEmpty()) {
            System.out.println("Either no ids bound to country or no location was given");
            System.out.println("Compare with all histograms ");
            return cmpHistogram(hist);
        }
        else {
            System.out.println("Comparing to a limited amount of histograms");
            return cmpHistogram(hist, ids);
        }
    }

    private String getBird(int ID){
        StringBuilder info = new StringBuilder();
        String name = ids.get(ID);
        String desc = birds.get(name);

        info.append(name + "\r\n\r\n");
        info.append(desc + "\r\n");

        return info.toString();
    }

    private List<String> getImageNames(final File folder) {
        List<String> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                continue;
            } else {
                files.add(folder + "/" + fileEntry.getName());
            }
        }
        return files;
    }

    private Mat[] loadImages(List<String> names){
        Mat[] images = new Mat[names.size()];

        for(int i = 0; i < names.size(); i++){
            images[i] = Imgcodecs.imread(names.get(i), BufferedImage.TYPE_3BYTE_BGR);

            //Imgproc.resize(images[i],images[i], new Size(50,50));
        }

        return images;
    }

    final int MY_METHOD = Imgproc.CV_COMP_CORREL;
    final int COLOR = Imgproc.COLOR_BGR2HSV;

    private int cmpHistogram(Mat[] src, Set<Integer> ids){
        int index = -1;
        double highest = 0.0;

        for(int i: ids){
            Mat[] ref = histograms.get(i);
            double sim = Imgproc.compareHist(ref[0], src[0], MY_METHOD);
            double sim2 = Imgproc.compareHist(ref[1], src[1], MY_METHOD);

            System.out.println("Comparing with " + i + " H: " + sim + " S:" + sim2 + " HS:" + ((sim+sim2)/2) );
            if(sim > highest && sim > 0.5 ){
                highest = sim;
                index = i;
            }
        }
        if(highest > 0)
            System.out.println("Best match had a similarity of " + ((int)(highest*100)) + "%");
        return index;
    }

    private int cmpHistogram(Mat[] src){
        int index = -1;
        double highest = 0.0;

        for(int i = 0; i < histograms.size(); i++){
            Mat[] ref = histograms.get(i);
            double sim = Imgproc.compareHist(ref[0], src[0], MY_METHOD);
            double sim2 = Imgproc.compareHist(ref[1], src[1], MY_METHOD);

            System.out.println("Comparing with " + i + " H: " + sim + " S:" + sim2 + " HS:" + ((sim+sim2)/2) );
            if(sim > highest && sim > 0.5 ){
                highest = sim;
                index = i;
            }
        }
        if(highest > 0)
            System.out.println("Best match had a similarity of " + ((int)(highest*100)) + "%");
        return index;
    }

    private Mat[] calcHistogram(Mat src){
        Mat[] dst = new Mat[]{ new Mat(), new Mat()};

        Imgproc.cvtColor(src, src, COLOR);
        MatOfInt sizes = new MatOfInt(24,24);
        MatOfInt ch1 = new MatOfInt(0,0);
        MatOfInt ch2 = new MatOfInt(1,1);
        MatOfFloat ranges = new MatOfFloat(0f,180f, 0f, 256f);

        Imgproc.calcHist( Arrays.asList(src), ch1, new Mat(), dst[0], sizes, ranges);
        Imgproc.calcHist( Arrays.asList(src), ch2, new Mat(), dst[1], sizes, ranges);

        Core.normalize(dst[0], dst[0], 1, 0, Core.NORM_MINMAX, -1, new Mat());
        Core.normalize(dst[1], dst[1], 1, 0, Core.NORM_MINMAX, -1, new Mat());
        return dst;
    }

    private Mat[] getImages(String foldername){
        File folder = new File(foldername);
        List<String> names = getImageNames(folder);
        System.out.println(names.toString());
        Mat[] refs = loadImages(names);
        return refs;
    }

    private List<Mat[]> calcHistograms(Mat[] refs){
        List<Mat[]> hist = new ArrayList<>();
        for(int i = 0; i < refs.length; i++){
            //Core.normalize(refs[i], refs[i], 0, 20, Core.NORM_MINMAX, -1, new Mat());
            Mat[] histograms = calcHistogram(refs[i]);
            hist.add(histograms);
        }

        return hist;
    }
}
