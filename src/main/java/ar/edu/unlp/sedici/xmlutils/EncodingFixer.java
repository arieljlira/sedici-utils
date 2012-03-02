package ar.edu.unlp.sedici.xmlutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.unlp.sedici.ioutils.input.FileInputStream;
import ar.edu.unlp.sedici.ioutils.output.DeferredFileOutputStream;

public class EncodingFixer {
	private static Logger log = LoggerFactory.getLogger(EncodingFixer.class);

	/**
	 * Retorna una copia del IS recibido con el encoding fixed
	 * @param is
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static InputStream fix(InputStream is, String encoding) throws UnsupportedEncodingException, IOException {
		final Fixer fixer = getFixer(encoding);

		Reader reader = new BufferedReader(new InputStreamReader(is, encoding));

		DeferredFileOutputStream dfos = new DeferredFileOutputStream(1024, "encodingFixer_", ".tmp", null);
		Writer writer = new FilterWriter(new BufferedWriter(new OutputStreamWriter(dfos, encoding))) {
			@Override
			public void write(String str, int off, int len) throws IOException {
				str = fixer.fix(str);
				super.write(str, off, len);
			}

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				String str = fixer.fix(new String(cbuf, off, len));
				cbuf = str.toCharArray();
				super.write(cbuf, 0, cbuf.length);
			}
		};

		IOUtils.copy(reader, writer);
		IOUtils.closeQuietly(writer);
		IOUtils.closeQuietly(reader);

		InputStream result_is;
		if (dfos.isInMemory())
			result_is = dfos.getMemoryOutputStream().toInputStream();
		else
			result_is = new FileInputStream(dfos.getFile());

		return result_is;
	}

	private static Fixer getFixer(String encoding) throws UnsupportedEncodingException {
		Fixer fixer;
		Charset requestedEncoding = Charset.forName(encoding);
		if (Charset.forName("UTF-8").equals(requestedEncoding))
			fixer = new Utf8EncodingFixer();
		else
			throw new UnsupportedEncodingException("The encoding Fixer does not support encoding " + encoding);
		return fixer;
	}

	private static interface Fixer {
		public String fix(String s);
	}

	private static class Utf8EncodingFixer implements Fixer {

		/**
		 * This method ensures that the output String has only valid XML unicode
		 * characters as specified by the XML 1.0 standard. For reference,
		 * please see the standard. This method will return an empty String if
		 * the input is null or empty.
		 * 
		 * @author Donoiu Cristian, GPL
		 * @param The
		 *            String whose non-valid characters we want to remove.
		 * @return The in String, stripped of non-valid characters.
		 * @see http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
		 */
		public String fix(String s) {
			StringBuilder out = new StringBuilder();
			int codePoint;

			int i = 0;
			while (i < s.length()) {
				codePoint = s.codePointAt(i);

				if (((codePoint >= 0x20) && (codePoint <= 0xD7FF)) || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
						|| ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)) || (codePoint == 0x9) || (codePoint == 0xA)
						|| (codePoint == 0xD)) {

					out.append(Character.toChars(codePoint));
				} else {
					out.append('\uFFFD');
					log.warn("Se encontró un caracter inválido en el XML de Origen: 0x" + Integer.toHexString(codePoint).toUpperCase());
				}
				i += Character.charCount(codePoint); // Increment with the
														// number of code
														// units(java chars)
														// needed to represent a
														// Unicode char.
			}
			return out.toString();
		}

	}

	public static void main(String[] args) throws IOException {

		String s = "hola\n sdfdsf \n.\u0019.\u0020.\uFFFD.sadf";
		InputStream Tosanitize = new ByteArrayInputStream(s.getBytes("UTF-8"));

		InputStream sanitized = EncodingFixer.fix(Tosanitize, "UTF-8");

		System.out.println(IOUtils.toString(sanitized, "UTF-8"));
	}
}
