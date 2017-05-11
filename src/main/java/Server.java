

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by steve_000 on 2017-05-03.
 */
public class Server{
    private ServerSocket serverSocket;
    private Set<UUID> history;
    public static Detector detector;

    public Server() throws IOException {
        this(4444);
    }

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        history = new HashSet<>();
        detector = new Detector();
    }

    public void serve(){
        System.err.println("Server up and ready");
        try {
            while(true) {
                UUID uuid = UUID.randomUUID();
                while(history.contains(uuid))
                    uuid = UUID.randomUUID();
                history.add(uuid);

                Thread tmp = new Thread(new ServiceProvider(serverSocket.accept(), uuid, detector));
                tmp.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        int port = 4444;
        if(args.length > 0){
            for(String s: args){
                String[] arg = s.split("=");
                switch (args[0]){
                    case "-p":
                    case "-port":
                        port = Integer.parseInt(args[1]);
                        break;
                    case "-h":
                    case "-help":
                    default:
                        System.err.println("Faulty argument");
                        System.exit(1);
                }
            }
        }

        try {
            new Server(port).serve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class ServiceProvider implements Runnable{
    private UUID session;
    private Socket cs;
    private Detector detector;
    private BufferedReader reader;
    private BufferedOutputStream writer;

    public ServiceProvider(Socket cs, UUID session, Detector detector) throws IOException {
        this.cs = cs;
        this.session = session;
        this.detector = detector;

        reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));
        writer = new BufferedOutputStream(cs.getOutputStream());
    }

    final int MAX_WIDTH = 768;

    @Override
    public void run() {
        String input;
        String output;
        JSONObject jo;
        try {
            while((input = read()) != null){
                if(input.equals(""))
                    break;

                jo = new JSONObject(input);

                System.out.print("Client> ");
                StringBuilder sb = new StringBuilder();
                for(String str: jo.keySet()){
                    if(str.equals("image"))
                        continue;

                    if(sb.length() > 0)
                        sb.append(", ");

                    sb.append(str + ":" + jo.getString(str));
                }


                System.out.println(sb.toString());

                output = jo.getString("image");
                output = output.replaceAll("\\s+","");
                byte[] imgbt = Base64.getDecoder().decode(output);
                ByteArrayInputStream bais = new ByteArrayInputStream(imgbt);
                BufferedImage img = ImageIO.read(bais);

                if(img.getWidth() > MAX_WIDTH){
                    int h = img.getHeight()*(MAX_WIDTH/img.getWidth());
                    BufferedImage resizedImage = new BufferedImage(MAX_WIDTH, h, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g = resizedImage.createGraphics();
                    g.drawImage(img, 0, 0, MAX_WIDTH, h, null);
                    g.dispose();

                    img = resizedImage;
                }

                // Getting location string
                String[] loc = jo.getString("location").split(",");
                if(loc.length > 1)
                    jo = detector.analyze(img, loc[1].replaceAll(" ", ""));
                else
                    jo = detector.analyze(img, loc[0].replaceAll(" ", ""));

                File outputfile = new File("Recieved.jpg");
                ImageIO.write(img, "jpg", outputfile);

                write(jo.toString());
            }

            System.out.println("Session done");
        } catch (IOException e) {
            System.out.println("Session broke due error " + e.getCause());
        }
    }

    private String read(){
        String str;
        StringBuilder sb = new StringBuilder("");
        try {
            while ((str = reader.readLine()) != null) {
                if(str.equals(""))
                    break;

                sb.append(str);
            }
        }catch (IOException e){
            return null;
        }
        return sb.toString();
    }

    private void write(String str) throws IOException {
        str += "\r\n\r\n";
        writer.write(str.getBytes());
        writer.flush();
    }


}
