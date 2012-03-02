package ar.edu.unlp.sedici.xmlutils;

public class TransformationException extends Exception {

	private static final long serialVersionUID = 1L;

	public TransformationException(String message) {
		super(message, null);
	}

	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
