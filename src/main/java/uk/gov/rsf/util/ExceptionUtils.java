package uk.gov.rsf.util;

public class ExceptionUtils {
    @SuppressWarnings("unchecked")
    private static <T extends Exception> void throwAsUnchecked(Throwable exception) throws T {
        throw (T) exception;
    }

    public static <M, T extends Exception> M rethrow(T ex) {
        throwAsUnchecked(ex);
        return null;
    }
}
