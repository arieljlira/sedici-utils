package ar.edu.unlp.sedici.xmlutils;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ar.edu.unlp.sedici.ioutils.input.RewindableInputStream;

import com.sun.org.apache.xerces.internal.parsers.XMLDocumentParser;
import com.sun.org.apache.xerces.internal.util.SAXInputSource;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;

//Si aun no se [puede detectar el encoding, probar http://sourceforge.net/projects/jchardet/files/
public class EncodingDetector {
    
	private static Logger log = LoggerFactory.getLogger(EncodingDetector.class);
	
    private EncodingDetector() {
		super();
	}

    /**
     * Este metodo intenta detectar el encoding del inputstream recibido como
     * parametro. 
     * @return
     * @throws IOException
     */
    public static String getEncoding(RewindableInputStream inputStream) throws IOException{
    	String encoding = null;
    	try {
			encoding = useXNI(new InputSource(inputStream));
		} catch (XNIException e) {
			log.error("XNIException: {}",e);
		}
		inputStream.rewind();
		if (encoding != null)
			return encoding;
		
		try {
			encoding = useSax(new InputSource(inputStream));
		} catch (SAXException e) {
			log.error("SaXException: {}",e);
		}
		inputStream.rewind();
		
		return encoding;
    }
    
    private static String useXNI(InputSource inputSource) throws XNIException, IOException {
    	AdvancedXNIEncodingDetector x = new AdvancedXNIEncodingDetector();
	
    	try{
    		x.parse(new SAXInputSource(inputSource));
	    } catch (XNIException e) {
			//no hago nada porque es la captura que de la excepcion que yo lance
		}
		String encoding = x.getActualEncoding();
		if (encoding == null)
			encoding = x.getDeclaredEncoding();
		return encoding;
    }
    
    private static String useSax(InputSource inputSource) throws IOException, SAXException {
    	XMLReader parser = XMLReaderFactory.createXMLReader();
		SAXEncodingDetector handler = new SAXEncodingDetector();
		parser.setContentHandler(handler);
		try {
			parser.parse(inputSource);
		} catch (SAXException e) {
			//no hago nada porque es la captura que de la excepcion que yo lance
		}
		return handler.getEncoding();
		
    }
    
    private static class AdvancedXNIEncodingDetector extends XMLDocumentParser {
    	private String actualEncoding = null;
        private String declaredEncoding = null;
        
	    @Override
	    public void startDocument(XMLLocator locator, String encoding, 
	        NamespaceContext namespaceContext, Augmentations augs)
	                throws XNIException {
	        actualEncoding = encoding;
	        declaredEncoding = null; // reset
	    }
	
	    @Override
	    // this method is not called if there's no XML declaration
	    public void xmlDecl(String version, String encoding, 
	      String standalone, Augmentations augs) throws XNIException {
	        declaredEncoding = encoding;
	    }
	
	    @Override
	    public void startElement(QName element, XMLAttributes attributes, 
	      Augmentations augs) throws XNIException {
	         throw new XNIException("DONE");
	    }
	    
	    String getActualEncoding() {
			return actualEncoding;
		}
	    String getDeclaredEncoding() {
			return declaredEncoding;
		}
    }
    
    private static class SAXEncodingDetector extends DefaultHandler {
    	private String encoding = null;
        private Locator2 locator;

    	@Override
    	public void setDocumentLocator(Locator locator) {
    		if (locator instanceof Locator2) {
    			this.locator = (Locator2) locator;
    		} else {
    			encoding = null;
    		}
    	}
    	
    	@Override
    	public void startDocument() throws SAXException {
    		if (locator != null) {
    			encoding = locator.getEncoding();
    		}
    		throw new SAXException("DONE");
    	}

    	String getEncoding() {
			return encoding;
		}
    }
} 