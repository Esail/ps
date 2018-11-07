import com.google.common.collect.Lists;
import data.Feature;
import data.Parser;

import java.util.List;

public class MnistParser implements Parser {
    @Override
    public List<Feature> parse(String line) {
        List<Feature> result = Lists.newArrayList();
        String[] cols = line.split(",");
        for (int i=0; i<cols.length; i++) {
            result.add(new Feature(i, Float.parseFloat(cols[i])));
        }
        return result;
    }
}
