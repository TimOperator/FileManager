package source;

public class Directory {

    private final String name;
    private final Long size;

    public Directory(String name, Long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }
}
