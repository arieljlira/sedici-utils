package ar.edu.unlp.sedici.xmlutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ar.edu.unlp.sedici.ioutils.input.FileInputStream;
import ar.edu.unlp.sedici.ioutils.input.RewindableInputStream;

public class XmlResource {
	private InputSource inputSource;
	private boolean lazyParser = true;
	private boolean fixEncodedChars = true;
	private Document document = null;
	private SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private ThreadLocal<XPathFactory> xPathFactory = new ThreadLocal<XPathFactory>() {
		@Override
		protected XPathFactory initialValue() {
			return XPathFactory.newInstance();
		}
	};

	private ThreadLocal<DocumentBuilder> documentBuilder = new ThreadLocal<DocumentBuilder>() {
		@Override
		protected DocumentBuilder initialValue() {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				dbf.setValidating(false);
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				// this should never happen with a well-behaving JAXP
				// implementation.
				throw new RuntimeException("No se pudo crear el DocumentBuilder, esto es muuuy raro", e);
			}
		}
	};

	private XmlResource(boolean lazyParserEnabled, boolean fixEncodedChars) {
		this.lazyParser = lazyParserEnabled;
		this.fixEncodedChars = fixEncodedChars;
	}
	
	public XmlResource(RewindableInputStream is) throws IOException, XmlProcessingException{
		this(true, true);
		this.setSource(is);
	}
	
	public XmlResource(InputStream is) throws IOException, XmlProcessingException{
		this(new RewindableInputStream(is));
	}
	

	public XmlResource(File file) throws IOException, XmlProcessingException {
		this(new FileInputStream(file));
	}

	protected void setSource(RewindableInputStream is) throws IOException, XmlProcessingException {
	
		String encoding = XmlUtils.getEncoding(is);
		
		this.setSource(is, encoding);
	}

	private void setSource(RewindableInputStream is, String encoding) throws IOException, XmlProcessingException {
		//trata de corregir el is en base al encoding
		if (this.fixEncodedChars){
			try{
				is = new RewindableInputStream(EncodingFixer.fix(is, encoding));
			}catch (UnsupportedEncodingException e) {
				log.warn("El encoding detectado ({}) no es soportado por el EncodingFixer, se deja el inputstream como viene", encoding);
			}
		}

		this.inputSource = new InputSource(is);
		this.inputSource.setEncoding(encoding);
		
		if (!lazyParser)
			this.getParsedDocument();
	}

	public void addNamespaceBindings(Map<String, String> binding) {
		this.namespaceContext.setBindings(binding);
	}

	public String evalXpathToString(String xpath) throws XmlProcessingException, IOException {
		return (String) this.evalXpath(xpath, XPathConstants.STRING);
	}

	public Double evalXpathToNumber(String xpath) throws XmlProcessingException, IOException {
		return (Double) this.evalXpath(xpath, XPathConstants.NUMBER);
	}

	public Boolean evalXpathToBoolean(String xpath) throws XmlProcessingException, IOException {
		return (Boolean) this.evalXpath(xpath, XPathConstants.BOOLEAN);
	}

	public NodeList evalXpathToNodeList(String xpath) throws XmlProcessingException, IOException {
		return (NodeList) this.evalXpath(xpath, XPathConstants.NODESET);
	}

	public Node evalXpathToNode(String xpath) throws XmlProcessingException, IOException {
		return (Node) this.evalXpath(xpath, XPathConstants.NODE);
	}
	
	public Calendar evalXpathToCalendar(String xpath) throws XmlProcessingException, IOException {
		String datetime  =this.evalXpathToString(xpath);
		try{
			return XmlUtils.convertDatetimeToCalendar(datetime);
		}catch (IllegalArgumentException e) {
			throw new XmlProcessingException("La fecha recuperada ("+datetime+")por el xpath "+xpath + "no puede ser transformada a un Calendar",e);
		}
	}
	

	// internos

	protected boolean isLazyParserEnabled() {
		return this.lazyParser;
	}

	protected boolean isDocumentParsed() {
		return this.lazyParser && (this.document == null);
	}

	public Document getParsedDocument() throws XmlProcessingException,IOException {
		if (this.document == null) {
			try {
				this.document = this.documentBuilder.get().parse( this.inputSource );
			} catch (SAXException e) {
				throw new XmlProcessingException("No se pudo procesar el XML, Error de SAX: "+e.getMessage(),e);
			}
		}
		return this.document;
	}


	private Object evalXpath(String xpath, QName returnType) throws XmlProcessingException, IOException {
		XPath xpathObject = xPathFactory.get().newXPath();
		xpathObject.setNamespaceContext(this.namespaceContext);
		Node contextNode = this.getParsedDocument();
		try {
			return xpathObject.evaluate(xpath, contextNode, returnType);
		} catch (XPathExpressionException e) {
			throw new XmlProcessingException("Error al ejecutar el xpath " + xpath, e);
		} catch (IllegalArgumentException e) {
			throw new XmlProcessingException(
					"Error al ejecutar el xpath, returnType is not one of the types defined in {@link XPathConstants}, sino  " + returnType,
					e);
		}
	}

	public RewindableInputStream getInputStream() throws IOException {
		RewindableInputStream is = (RewindableInputStream) inputSource.getByteStream();
		is.rewind();
		return is;
	}

	public String getEncoding() {
		return this.inputSource.getEncoding();
	}
	
	public static void main(String[] args) throws IOException, XmlProcessingException {
		InputStream inputStream = IOUtils.toInputStream("<?xml version='1.0'?>\n<book name=\"pepe\">" +
				"<author>Gambardella, Matthew</author>" +
				"<title>XML Developer's Guide</title>" +
				"<genre>Computer</genre></book>");
		XmlResource resource = new XmlResource(inputStream);
		String result = resource.evalXpathToString("//@name");
		System.out.println(result);
	}
	
}