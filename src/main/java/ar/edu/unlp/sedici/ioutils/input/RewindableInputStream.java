package ar.edu.unlp.sedici.ioutils.input;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import ar.edu.unlp.sedici.ioutils.output.DeferredFileOutputStream;


public class RewindableInputStream extends BaseProxyInputStream{
 
	protected static int THRESHOLD = 8192; 
	
	public static void setThreshold(int threshold){
		if (threshold <= 0) throw new IllegalArgumentException("El threshold debe ser mayor a cero");
		
		THRESHOLD = threshold;
	}
	
	protected static InputStream rewindInputStream(InputStream masterInputStream) throws IOException{
		if(masterInputStream instanceof FileInputStream)
			return new FileInputStream(( (FileInputStream) masterInputStream).getFile() );
		else if(masterInputStream instanceof ByteArrayListInputStream)
			return new ByteArrayListInputStream( (ByteArrayListInputStream) masterInputStream );
		else if(masterInputStream instanceof RewindableInputStream) {
			((RewindableInputStream) masterInputStream).rewind();
			return masterInputStream;
		} else {
			InputStream result_is;
			DeferredFileOutputStream os = new DeferredFileOutputStream(THRESHOLD , "rwis_", ".tmp", null);
			
			IOUtils.copy(masterInputStream, os);
			if (os.isInMemory())
				result_is = os.getMemoryOutputStream().toInputStream();
			else 
				result_is = new FileInputStream(os.getFile());
			
			return result_is;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	private final InputStream masterInputStream;

	private ThreadLocal<InputStream> source = new ThreadLocal<InputStream>() {
		@Override
		protected InputStream initialValue() {
			try {
				return rewindInputStream(masterInputStream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	public RewindableInputStream(InputStream masterInputStream) throws IOException {
		super();
		this.masterInputStream = rewindInputStream(masterInputStream);
	}

	public void rewind() throws IOException {
		source.remove();
	}

	@Override
	protected InputStream getBackingInputStream() {
		return source.get();
	}
	
}