package ar.edu.unlp.sedici.xmlutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

import ar.edu.unlp.sedici.ioutils.input.FileInputStream;
import ar.edu.unlp.sedici.ioutils.input.RewindableInputStream;
import ar.edu.unlp.sedici.ioutils.output.DeferredFileOutputStream;

public class XmlUtils {

	private static Logger log = LoggerFactory.getLogger(XmlUtils.class);
	
	public static Document getDocument(Node node) {
		if(node instanceof Document)
			return (Document)node;
		else
			return node.getOwnerDocument();
	}
	
	public static Document getDocument(InputStream is) throws XmlProcessingException, IOException {
		InputSource iss = null;
		if(is != null)
			iss = new InputSource(is);
		return getDocument(iss);
	}
	
	public static Document getEmptyDocument() throws XmlProcessingException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		Document doc = null;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		}catch(ParserConfigurationException e) {
			throw new XmlProcessingException("Error de Configuracion del Parser", e);
		}
		return doc;
	}
	
	public static Document getDocument(InputSource source) throws XmlProcessingException, IOException {
		if (source == null) return null;
			
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		Document doc = null;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(source);
		}catch(ParserConfigurationException e) {
			throw new XmlProcessingException("Error de Configuracion del Parser", e);
		}catch(SAXException e) {
			throw new XmlProcessingException("Error de SAX", e);
		}
		return doc;
	}
	

	public static Source getSource(Node node){ return new DOMSource(node); }
	public static Source getSource(File file){ return new StreamSource(file); }
	public static Source getSource(InputStream is){ return new StreamSource(is); }
	public static Source getSource(Reader r) { return new StreamSource(r); }
	public static Source getSource(String s) { return new StreamSource(new StringReader(s)); }
	
	public static Result getResult(Node node){ return new DOMResult(node); }
	public static Result getResult(File file){ return new StreamResult(file); }
	public static Result getResult(OutputStream os){ return new StreamResult(os); }
	public static Result getResult(Writer w) { return new StreamResult(w); }
	public static Result getResult(StringBuilder buf) { return new StreamResult(new StringBuilderWriter(buf)); }
	
	public static void writeTo(Source source, Result destination) throws TransformerException  {
		applyTransformation(source, null, destination, null);
	}
	
	
	public static XmlResource transformXmlResource(Source xsl, XmlResource source) throws TransformationException {
		return XmlUtils.transformXmlResource(xsl, source, null);
	}
	
	public static XmlResource transformXmlResource(Source xsl, XmlResource source, Map<String, Object> extraParams) throws TransformationException {
		Source xmlSource;
		try {
			xmlSource = XmlUtils.getSource( source.getInputStream() );
		} catch (Exception e) {
			throw new TransformationException("Error parseando documento XML: "+e.getMessage(), e);
		}
		return XmlUtils.transformXmlSource(source.getEncoding(), xsl, xmlSource, extraParams);
	}


	
	public static XmlResource transformXmlSource(String outputEncoding, Source xsl, Source source, Map<String, Object> extraParams) throws TransformationException {
		// Preparacion del Xml de destino
		DeferredFileOutputStream resultOutputStream = new DeferredFileOutputStream(1024, "transformed_", ".xml", null);
		Result destinationXml = XmlUtils.getResult(resultOutputStream);
		
		// Aplica la transformacion
		try {
			XmlUtils.applyTransformation(outputEncoding, source, xsl, destinationXml, extraParams);
		} catch (TransformerException e) {
			log.error(e.getClass().getSimpleName()+":"+e.getMessage());
			throw new TransformationException("Error durante la transformacion", e);
		}
		
		try {
			resultOutputStream.close();
			return XmlUtils.getXmlResource(resultOutputStream.getInputStream());
		} catch (FileNotFoundException e) {
			throw new TransformationException("Error intentando leer del archivo de resultado", e);
		} catch (IOException e) {
			throw new TransformationException("Error intentado cerrar el buffer con la salida de la transformacion", e);
		}
	}
	

	public static XmlResource getXmlResource(Node element) throws TransformationException {
		Source xmlSource = XmlUtils.getSource(element);
		return transformXmlSource("utf-8", null, xmlSource, null);
	}
	
	public static XmlResource getXmlResource(InputStream inputStream) throws TransformationException {
		try {
			return new XmlResource(inputStream);
		} catch (IOException e) {
			throw new TransformationException("Error intentando leer del archivo de resultado", e);
		} catch (XmlProcessingException e) {
			throw new TransformationException("Error procesando el archivo de resultado", e);
		}
	}
	
	public static TransformationReport applyTransformation(Source sourceXml, Source xslt, Result destinationXml , Map<String, Object> xslparams) throws TransformerException  {
		return applyTransformation(Charset.defaultCharset().name(), sourceXml, xslt, destinationXml, xslparams);
	}	    
	
	public static TransformationReport applyTransformation(String outputEncoding, Source sourceXml, Source xslt, Result destinationXml , Map<String, Object> xslparams) throws TransformerException  {
        TransformerFactory xformFactory = TransformerFactory.newInstance();
		
        Transformer idTransformer;
        if (xslt == null)
        	idTransformer= xformFactory.newTransformer();
        else
        	idTransformer= xformFactory.newTransformer(xslt);
        TransformationReport report = new TransformationReport();
        idTransformer.setErrorListener(report);
    
        if (xslparams != null)
	        for (String paramName : xslparams.keySet()) {
	        	idTransformer.setParameter(paramName, xslparams.get(paramName));	
			}
        
        idTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        idTransformer.setOutputProperty(OutputKeys.ENCODING, outputEncoding);
        idTransformer.setOutputProperty(OutputKeys.INDENT, "no");
        
        
        long start = System.currentTimeMillis();
        idTransformer.transform(sourceXml, destinationXml);
        
        log.trace("Se aplico la transformacion en {} milisegundos", (System.currentTimeMillis() - start));
        
        if (report.getErrors().size() != 0 ){
        	StringBuffer buf = new StringBuffer("Se produjo un error al tratar de escribir el document el la salida. ");
			report.printTo(buf, false);
			throw new TransformerException(buf.toString());
		}
		
        return report;

	}

	/**
	 * 
	 * @param inputSream
	 * @return el encoding del inputSource o null si no se lo pudo detectar
	 * @throws IOException
	 */
	public static String getEncoding(RewindableInputStream inputSream) throws IOException{
		return EncodingDetector.getEncoding(inputSream);
	}
	
	public static TransformationReport getErrorListener(){
		return new TransformationReport();
	}
	
	/**
	 * 
	 * @author ariel
	 *
	 */
	public static class TransformationReport implements ErrorListener {

		private int transformedRecords = 0;
		private List<String> errors = new LinkedList<String>();
		private List<String> warnings = new LinkedList<String>();

		public List<String> getErrors() {
			return errors;
		}

		public List<String> getWarnings() {
			return warnings;
		}

		public void error(TransformerException exception)
				throws TransformerException {
			this.warnings.add(exception.getMessageAndLocation());
			//continua la ejecucion normal
		}

		public void fatalError(TransformerException exception)
				throws TransformerException {
			this.errors.add(exception.getMessageAndLocation());
			throw exception;
		}

		public void warning(TransformerException exception)
				throws TransformerException {
			this.errors.add("FATAL! " + exception.getMessageAndLocation());
			throw exception;
		}

		public void printTo(StringBuffer buf, boolean showWarnings) {
			if (showWarnings && !this.warnings.isEmpty())
				buf.append("\tWarnings: ");buf.append(this.warnings);
			
			if (!this.errors.isEmpty())
				buf.append("\tErrors: ");buf.append(this.errors);
			
		}
		
		public void mergeReport(TransformationReport partialReport) {
			this.transformedRecords += partialReport.getTransformedRecordsCount();
			this.errors.addAll(partialReport.getErrors());
			this.warnings.addAll(partialReport.getWarnings());
		}

		private int getTransformedRecordsCount() {
			return this.transformedRecords;
		}
	}


	/**
     * This method ensures that the output String has only valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see the
     * standard. This method will return an empty String if the input is null or empty.
     *
     * @author Donoiu Cristian, GPL
     * @param  The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String removeInvalidXMLCharacters(String s) {
    	
        StringBuilder out = new StringBuilder();
    	int codePoint;
    	
    	int i=0;
    	while(i<s.length()) {
    		codePoint = s.codePointAt(i);

			if (((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
					((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
					((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)) || 
					(codePoint == 0x9) || (codePoint == 0xA) ||	(codePoint == 0xD)) {
				
				out.append(Character.toChars(codePoint));
			} else {
				log.warn("Se encontró un caracter inválido en el XML de Origen: 0x" + Integer.toHexString(codePoint).toUpperCase());
			}
			i+= Character.charCount(codePoint);                 // Increment with the number of code units(java chars) needed to represent a Unicode char.  
    	}
    	return out.toString();
    } 
    
    public static Calendar convertDatetimeToCalendar(String datetime_str){
		 return XMLGregorianCalendarImpl.parse(datetime_str).toGregorianCalendar();
	}
	
	public static Date convertDatetimeToDate(String datetime_str){
		return convertDatetimeToCalendar(datetime_str).getTime();
	}
}
