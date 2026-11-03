package api;

public enum Headers {
    AUTH_HEADER("Authorization");

    Headers(String header) {
        this.header = header;
    }

    private final String header;

    public String getValue() {
        return header;
    }
}
