package beam.utils.csv.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public interface ICsvReader {

    Map<String, Map<String, String>> read(BufferedReader bufferedReader) throws IOException;
}
