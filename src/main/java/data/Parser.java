package data;

import java.util.List;

public interface Parser {
    List<Feature> parse(String line);
}
