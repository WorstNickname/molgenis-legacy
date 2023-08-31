package org.molgenis.model;

public class ModelExtractionException extends RuntimeException {

    public ModelExtractionException(String message) {
        super(message);
    }

    public ModelExtractionException(Throwable cause) {
        super(cause);
    }
}
