package utils;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SpectrumStaticLoader {


    public static class SpectrumData {

        public List<Entry> intensitySample = new ArrayList<>();
        public List<Entry> intensityRef = new ArrayList<>();
        public List<Entry> absorbance = new ArrayList<>();
        public List<Entry> reflectance = new ArrayList<>();

    }

    public static SpectrumData loadAllData(InputStream inputStream) {
        SpectrumData data = new SpectrumData();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isData = false;
            while ((line = reader.readLine()) != null) {
                if(line.contains("Wavelength")) {isData = true; continue;}
                if(isData && !line.trim().isEmpty()) {
                    String[] col = line.split(",");
                    float x = Float.parseFloat(col[0]);
                    float abs = Float.parseFloat(col[1]);
                    float ref = Float.parseFloat(col[2]);
                    float sam = Float.parseFloat(col[3]);

                    data.absorbance.add(new Entry(x, abs));
                    data.intensitySample.add(new Entry(x, sam));
                    data.intensityRef.add(new Entry(x, ref));
//                    计算反射率: (Sample / Reference ) * 100
                    data.reflectance.add(new Entry(x, (sam / ref) * 100f));
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;

    }


}
