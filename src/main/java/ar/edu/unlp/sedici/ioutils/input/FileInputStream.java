package ar.edu.unlp.sedici.ioutils.input;

import java.io.File;
import java.io.FileNotFoundException;

public class FileInputStream extends java.io.FileInputStream {
	private File file;

	public FileInputStream(File file) throws FileNotFoundException {
		super(file);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}