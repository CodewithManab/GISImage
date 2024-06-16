package app.codewithmanab.geoimage.properties;

public class GEOException extends Exception {
    private int errorCode = 0;

    public GEOException(String message) {
        super(message);
    }

    public GEOException(String message, Throwable cause) {
        super(message, cause);
    }

    public GEOException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}