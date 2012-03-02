package ar.edu.unlp.sedici.ioutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;


public class FileUtils {
	
	public static final String FILE_EXTENSION_XML = ".xml";	
	public static final String REGEX_FILTER_XML_FILES = "(.*)"+FILE_EXTENSION_XML;
	
	
	public static boolean assertWriteableDirectory(File d, boolean createIfNotExists) throws FileException {
		String directory = d.getAbsolutePath(); 
		
		
		if (!d.exists()) {
			if(createIfNotExists) {
				if(!d.mkdirs())
					throw new FileException("El directorio " + directory + " no existe y no puede ser creado");
			} else {
				throw new FileException("El directorio " + directory + " no existe");
			}
		}
		
		if (!d.isDirectory())
			throw new FileException("El archivo " + directory + " debe ser un directorio");
		if (!d.canWrite())
			throw new FileException("El directorio " + directory + " debe ser writeable");
		
		return true;
	}

	/**
	 * @param f the file to check 
	 * @return true if the file exists and is readable. False if it doesn't exists
	 * @throws FileException If f is not a file or cannnot be read
	 */
	public static boolean assertReadableFile(File f) throws FileException {
		if (!f.exists())
			return false;
		
		if (!f.isFile())
			throw new FileException("El path " + f + " debe apuntar a un archivo");
		if (!f.canRead())
			throw new FileException("El archivo " + f + " debe ser readable");
		
		return true;
	}
	
	/**
	 * @param f the file to check 
	 * @return true if the file exists and is readable. False if it doesn't exists
	 * @throws FileException If f is not a file or cannnot be read
	 */
	public static boolean assertReadableDirectory(File f) throws FileException {
		if (!f.exists())
			return false;
		
		if (!f.isDirectory())
			throw new FileException("El path " + f + " debe apuntar a un directorio");
		if (!f.canRead())
			throw new FileException("El directorio " + f + " debe ser readable");
		
		return true;
	}

	public static boolean assertWriteableFile(File f) throws FileException {
		if (!f.exists())
			return false;
		
		if (!f.isFile())
			throw new FileException("El archivo " + f + " debe ser un archivo");
		if (!f.canWrite())
			throw new FileException("El archivo " + f + " debe ser readable");
		
		return true;
	}
	
	
	public static void removeDirectoryContents(File directory, String regexFilter, boolean recurse, boolean deleteFolders) throws FileException {
		assertWriteableDirectory(directory, false);
		
		//File[] files = directory.listFiles((FilenameFilter)new RegexFilenameFilter(regexFilter));
		File[] files = getFilesFromDirectory(directory, regexFilter);
		
		for (File file : files) {
			if (file.isDirectory()){
				if (recurse)
					removeDirectoryContents(directory, regexFilter,recurse, deleteFolders);
				if (!deleteFolders)
					continue;
			}
			if (!file.delete())
				throw new FileException("No se puede borrar el archivo " + file.getAbsolutePath());
		}
	}

	public static void removeDirectory(File directory, boolean forceIfNotEmpty) throws FileException {
		assertWriteableDirectory(directory.getParentFile(), false);

		// Verifica si tiene archivos dentro
		File[] files = getFilesFromDirectory(directory, "\\*");
		if(files.length != 0) {
			assertWriteableDirectory(directory, false);
			// Vacia el directorio
			if(forceIfNotEmpty) {
				removeDirectoryContents(directory, "\\*", true, true);
			}
		}
		
		// Elimina el directorio
		if (!directory.delete())
			throw new FileException("No se puede borrar el directorio " + directory.getAbsolutePath());
	}
	
	public static File[] getFilesFromDirectory(File directory, String regexFilter) {
		if (directory == null) throw new IllegalArgumentException("El directorio no puede ser null");
		return directory.listFiles((FilenameFilter)new RegexFilenameFilter(regexFilter));
	}
	
	private static class RegexFilenameFilter extends AbstractFileFilter{
		private Pattern regex;
		
		public RegexFilenameFilter(String regex) {
			this.regex = Pattern.compile(regex);
		}
		
		public boolean accept(File dir, String name) {
			return this.regex.matcher(name).matches();
		}
	}

	public static void readFileContents(File file, StringBuffer output, Charset charset) throws FileException {
		if (!FileUtils.assertReadableFile(file))
			throw new FileException("El archivo no existe " + file.getAbsolutePath());
		
		try {
			Reader r = new InputStreamReader(new FileInputStream(file), charset);
			FileUtils.readContents(r, output, charset);
		} catch (IOException e) {
			throw new FileException("Error de IO sobre el archivo " + file.getAbsolutePath() , e);
		}
	}
	
	public static void readContents(Reader r, StringBuffer output, Charset charset) throws IOException {
		char[] cbuf = new char[1024];
		int len = 0;
		while ((len = r.read(cbuf))>= 0){
			output.append(cbuf, 0 , len);
		}
		r.close();
	}

	
	public static boolean checkDirectoryStructure(File d, boolean create) throws FileException {
		
		String directory = d.getAbsolutePath();
		if (d.exists() && !d.isDirectory())
			throw new FileException("El archivo " + directory + " debe ser un directorio");
		else if(d.exists()) //es un directorio
			return true;
		else if (create){
			if (d.mkdirs())
				return true;
			else
				throw new FileException("No se pudo crear El directorio " + directory + "");
		}else
			return false;
	}
	 
	public static String checkSlashes(String filename, boolean addLastSlash){
		if (addLastSlash)
			return FilenameUtils.normalizeNoEndSeparator(filename) + File.separator;
		else
			return FilenameUtils.normalize(filename);
	} 
	
	public static String joinPaths(String... paths){
		StringBuilder buffer = new StringBuilder();
		for(String path : paths) {
			buffer.append( path );
			buffer.append( org.apache.commons.io.IOUtils.DIR_SEPARATOR );
		}
		return FilenameUtils.normalize( buffer.toString() );
	}
	
	public static String generateUniqueFilename(String directoryPath, String prefix, String suffix) {
		if(prefix == null)
			prefix = "";
		
		if(suffix == null)
			suffix = "";
		
		if(directoryPath == null)
			directoryPath = "";
		else
			directoryPath = directoryPath + IOUtils.DIR_SEPARATOR;
		
		String randomFilename = UUID.randomUUID().toString();
		return FilenameUtils.normalize( directoryPath + prefix + randomFilename + suffix );
	}

	public static void zipFiles(OutputStream os, List<File> logFiles) throws IOException {
		ZipOutputStream x = new ZipOutputStream(os);

		for (File file : logFiles) {
			System.out.println("Agrego al zip " + file.getAbsolutePath());
			x.putNextEntry(new ZipEntry(file.getName()));
			FileInputStream fis = new FileInputStream(file);
			IOUtils.copy(fis, x);
			x.closeEntry();
			fis.close();
		}

		x.finish();
		x.flush();
		//x.close();
		//os.close();
	}   
	
	public static BufferedReader getFileReader(InputStream configStream, String encoding) throws FileException, IOException {
		
		try {
			return new BufferedReader( new InputStreamReader(configStream, encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IOException("El encoding especificado "+encoding+" para leer el archivo de configuracion no es soportado/valido. "+e.getMessage(), e);
		}
	}

	public static InputStream getFileInputStream(File f) throws FileException{
		if (!assertReadableFile(f))
			throw new FileException("No se puede leer el archivo "+f.getAbsolutePath(), null);
		
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new FileException("No se encuentra el archivo "+f.getAbsolutePath(), null);
		}
	}
	
	public static InputStream getFileInputStream(String fileName, Class<?> clazz) throws FileException{
		InputStream configStream = clazz.getResourceAsStream(fileName);
		
		if(configStream == null){
			File f = new File(fileName);
			configStream = getFileInputStream(f) ;
		}
		return configStream;
			
	}
	
	public static Properties readPropertesFile(File f, String encoding) throws FileException, IOException{
		InputStream configStream = getFileInputStream(f);
		return readProperties(configStream, encoding);	
	}
	
	public static Properties readPropertesFile(String fileName, String encoding, Class<?> clazz) throws FileException, IOException{
		InputStream configStream = getFileInputStream(fileName, clazz);
		return readProperties(configStream, encoding);
	}

	public static Properties readProperties(InputStream configStream,  String encoding) throws FileException, IOException{
		Reader r= getFileReader(configStream,encoding);

		Properties fileProperties = new Properties();
		try {
			fileProperties.load(r);
			r.close();
		} catch (IOException e) {
			throw new FileException("Error leyendo el InputStream de configuracion: "+e.getMessage(), e);
		}
		return fileProperties;
	}
		
	
}


