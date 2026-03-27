package warehouse;

import java.util.EnumMap;
import java.util.Map;

// This class represents the whole warehouse
// It stores different sections for each item type
public class Warehouse {
    // map of item type -> section (e.g. electronics section, books section, etc.)
    private final Map<Type, Section> sections = new EnumMap<>(Type.class);
    // constructor sets up all sections
    public Warehouse(int capacity) {
        // create a section for each type of item
        for (Type t : Type.values()) {
            // each section has 5 stockers and a capacity 
            sections.put(t, new Section(5, capacity));
        }
    }
    // get the section for a specific item type
    public Section getSection(Type type) {
        return sections.get(type);
    }
}
