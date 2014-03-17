package com.test.bicycle.app.bicyclerouteplanner.app;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by olgakuklina on 3/16/2014.
 */
public class ElevationService {

    public double[]  getPath(List<LatLng> points){
       String stringPoints = pointsToString(points);
        Document doc = null;
        try {
            URL url = new URL("https://maps.googleapis.com/maps/api/elevation/xml?path=" + stringPoints +
                    "&samples=" + points.size() +"&sensor=true&key=AIzaSyDg19MqLy5HFt0ycY9fPUIDMwTDIcY2WWk");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.connect();

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(connection.getInputStream());

        } catch (Exception e) {
            Log.e("ElevationService", "", e);
            return null;
        }

        NodeList list = doc.getElementsByTagName("elevation");
        double[] elevations = new double[list.getLength()];
        for (int i = 0; i < list.getLength(); i++) {
            elevations[i] = Double.parseDouble(list.item(i).getTextContent());
        }
        return elevations;
    }

    private String pointsToString(List<LatLng> points) {
        StringBuilder stringBuilder = new StringBuilder();
        for (LatLng point : points){
            stringBuilder.append(point.latitude).append(",").append(point.longitude).append("|");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
