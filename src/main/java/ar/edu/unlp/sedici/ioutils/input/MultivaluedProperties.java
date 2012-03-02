package ar.edu.unlp.sedici.ioutils.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ar.edu.unlp.sedici.ioutils.FileException;
import ar.edu.unlp.sedici.ioutils.FileUtils;

public class MultivaluedProperties extends HashMap<String, List<String>> implements Map<String, List<String>>  {

	private static final long serialVersionUID = 1L;

	public MultivaluedProperties(File file) throws IOException, FileException {
		super();
		this.load(file, "utf-8");
	}

	public MultivaluedProperties() {
	}

	public void load(File file, String encoding) throws IOException, FileException {
		InputStream configStream = FileUtils.getFileInputStream(file);
		BufferedReader reader = FileUtils.getFileReader(configStream, encoding);
		
		this.load(reader);
	}
	
	public void load(BufferedReader r) throws IOException {
		
		String alias_line ;
		try {
			while ((alias_line = r.readLine())!= null){
				
				if (alias_line.indexOf('#')==0)
					continue;
				
				int i = alias_line.indexOf('=');
				if (i == -1)
					continue;
				String key  = alias_line.substring(0,i);
				if (!this.containsKey(key))
					this.put(key,new LinkedList<String>());
					
				this.get(key).add(alias_line.substring(i + 1));
			}
			
		} finally{
			r.close();
		}
		
	}

	@Override
	public List<String> get(Object key) {
		
		if (!this.containsKey(key))
			super.put((String)key, new ArrayList<String>());
		
		return super.get(key);
	}
	
	@Override
	public List<String> put(String key, List<String> value) {
		//System.out.println("me agregan en " + key + " los valores " + value );
		this.get(key).addAll(value);
		return null;
	}
	
	public List<String> put(String key, String ...value) {
		this.get(key).addAll(Arrays.asList(value));
		return null;
	}
	
	
}
