package beam.utils.csv.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class CsvToMap implements ICsvReader{

    @Override
    public Map<String, Map<String, String>> read(BufferedReader bufferedReader) throws IOException {

        String[] headers = null;
        Map<String, Map<String, String>> map = new HashMap<>();

        String line;

        int i = 0;

        while ((line = bufferedReader.readLine()) != null) {

            if(i == 0){
                headers = line.split(",");
            }else {
                Map<String, String> row = new HashMap<>();
                String[] data = line.split(",");

                for(int j=0; j<headers.length; j++){

                    row.put(headers[j], data[j]);
                }
                map.put(data[0], row);
            }

            i++;
        }

        return map;
    }
}
