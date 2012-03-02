package ar.edu.unlp.sedici.ioutils;


public class FileException extends Exception //IOException
{

	private static final long serialVersionUID = -204962240027469364L;

	public FileException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
	public FileException(String arg0) {
		super(arg0, null);
	}
	
}
