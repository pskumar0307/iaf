/*
 * $Log: AbstractXmlValidator.java,v $
 * Revision 1.1  2012-08-23 11:57:43  m00f069
 * Updates from Michiel
 *
 * Revision 1.6  2012/03/16 15:35:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added EsbSoapValidator and WsdlXmlValidator, made WSDL's available for all adapters and did a bugfix on XML Validator where it seems to be dependent on the order of specified XSD's
 *
 * Revision 1.5  2011/12/08 10:57:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.4  2011/11/30 13:51:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/09/13 13:39:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getLogPrefix()
 *
 * Revision 1.1  2011/08/22 09:51:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new baseclasses for XmlValidation
 *
 */
package nl.nn.adapterframework.util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.*;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.*;

/**
 * baseclass for validating input message against a XML-Schema.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method. (effectively the same as noNamespaceSchemaLocation)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>none</td></tr>
 * <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>none</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValidateFile(boolean) validateFile}</td><td>when set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file, only used when {@link #setValidateFile(boolean) validateFile} is <code>true</code></td><td>UTF-8</td></tr>
 * </table>
 * <br>
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * @version Id
 * @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)
 */
public abstract class AbstractXmlValidator {
	protected Logger log = LogUtil.getLogger(this);

	public static final String XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT = "Invalid XML: parser error";
	public static final String XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT = "Invalid XML: wrong root";
	public static final String XML_VALIDATOR_NOT_VALID_MONITOR_EVENT = "Invalid XML: does not comply to XSD";
	public static final String XML_VALIDATOR_VALID_MONITOR_EVENT = "valid XML";

    private String schemaLocation = null;
    private String noNamespaceSchemaLocation = null;
	private String schemaSessionKey = null;
    private boolean throwException = false;
    private boolean fullSchemaChecking = false;
	private String reasonSessionKey = null;
	private String xmlReasonSessionKey = null;
	private String root = null;
	private boolean validateFile=false;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
    protected boolean needsInit = true;
    protected String logPrefix = "";
    protected boolean addNamespaceToSchema = false;

    public boolean isAddNamespaceToSchema() {
        return addNamespaceToSchema;
    }

    public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
        this.addNamespaceToSchema = addNamespaceToSchema;
    }


    public class XmlErrorHandler implements ErrorHandler {
        private boolean errorOccured = false;
        private String reasons;
		private XMLReader parser;
		private XmlBuilder xmlReasons = new XmlBuilder("reasons");


		public XmlErrorHandler(XMLReader parser) {
			this.parser = parser;
		}

		public void addReason(String message, String location) {
			try {
				ContentHandler ch = parser.getContentHandler();
				if (ch!=null && ch instanceof XmlFindingHandler) {
					XmlFindingHandler xfh = (XmlFindingHandler)ch;

					XmlBuilder reason = new XmlBuilder("reason");
					XmlBuilder detail;

					detail = new XmlBuilder("message");;
					detail.setCdataValue(message);
					reason.addSubElement(detail);

					detail = new XmlBuilder("elementName");;
					detail.setValue(xfh.getElementName());
					reason.addSubElement(detail);

					detail = new XmlBuilder("xpath");;
					detail.setValue(xfh.getXpath());
					reason.addSubElement(detail);

					xmlReasons.addSubElement(reason);
				}
			} catch (Throwable t) {
				log.error(getLogPrefix(null)+"Exception handling errors",t);

				XmlBuilder reason = new XmlBuilder("reason");
				XmlBuilder detail;

				detail = new XmlBuilder("message");;
				detail.setCdataValue(t.getMessage());
				reason.addSubElement(detail);

				xmlReasons.addSubElement(reason);
			}

			if (StringUtils.isNotEmpty(location)) {
				message = location + ": " + message;
			}
			errorOccured = true;
			if (reasons == null) {
				 reasons = message;
			 } else {
				 reasons = reasons + "\n" + message;
			 }
		}

		public void addReason(Throwable t) {
			String location=null;
			if (t instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException)t;
				location = "at ("+spe.getLineNumber()+ ","+spe.getColumnNumber()+")";
			}
			addReason(t.getMessage(),location);
		}

		public void warning(SAXParseException exception) {
			addReason(exception);
		}
        public void error(SAXParseException exception) {
        	addReason(exception);
        }
        public void fatalError(SAXParseException exception) {
			addReason(exception);
        }

        public boolean hasErrorOccured() {
            return errorOccured;
        }

         public String getReasons() {
            return reasons;
        }

		public String getXmlReasons() {
		   return xmlReasons.toXML();
	   }
    }

    public static class XmlValidatorException extends IbisException {
    	XmlValidatorException(String cause, Throwable t) {
    		super(cause,t);
    	}
    	XmlValidatorException(String cause) {
    		super(cause);
    	}
    }
    /**
     * Configure the XmlValidator
     * @throws ConfigurationException when:
     * <ul><li>the schema cannot be found</li>
     * <ul><li><{@link #isThrowException()} is false and there is no forward defined
     * for "failure"</li>
     * <li>when the parser does not accept setting the properties for validating</li>
     * </ul>
     */
    public void configure(String logPrefix) throws ConfigurationException {
        this.logPrefix = logPrefix;
    }

    protected void init() throws ConfigurationException {
        if (needsInit) {
            if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
                StringUtils.isNotEmpty(getSchemaLocation())) &&
                StringUtils.isNotEmpty(getSchemaSessionKey())) {
                throw new ConfigurationException(logPrefix + "cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
            }
            if (StringUtils.isNotEmpty(getSchemaLocation())) {
                String resolvedLocations = XmlUtils.resolveSchemaLocations(getSchemaLocation());
                log.info(logPrefix + "resolved schemaLocation [" + getSchemaLocation() + "] to [" + resolvedLocations + "]");
                setSchemaLocation(resolvedLocations);
            }
            if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
                URL url = ClassUtils.getResourceURL(this, getNoNamespaceSchemaLocation());
                if (url == null) {
                    throw new ConfigurationException(logPrefix + "could not find schema at ["+getNoNamespaceSchemaLocation()+"]");
                }
                String resolvedLocation =url.toExternalForm();
                log.info(logPrefix + "resolved noNamespaceSchemaLocation to [" + resolvedLocation+"]");
                setNoNamespaceSchemaLocation(resolvedLocation);
            }
            if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
                StringUtils.isEmpty(getSchemaLocation()) &&
                StringUtils.isEmpty(getSchemaSessionKey())) {
                throw new ConfigurationException(logPrefix + "must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
            }
            needsInit = false;
        }
    }

	protected String handleFailures(XmlErrorHandler xeh, IPipeLineSession session, String mainReason, String forwardName, String event, Throwable t) throws  XmlValidatorException {

		String fullReasons=mainReason;
		if (StringUtils.isNotEmpty(xeh.getReasons())) {
			if (StringUtils.isNotEmpty(mainReason)) {
				fullReasons+=":\n"+xeh.getReasons();
			} else {
				fullReasons=xeh.getReasons();
			}
		}
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(getLogPrefix(session)+"storing reasons under sessionKey ["+getReasonSessionKey()+"]");
			session.put(getReasonSessionKey(),fullReasons);
		}
		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(getLogPrefix(session)+"storing reasons (in xml format) under sessionKey ["+getXmlReasonSessionKey()+"]");
			session.put(getXmlReasonSessionKey(),xeh.getXmlReasons());
		}
		if (isThrowException()) {
			throw new XmlValidatorException(fullReasons, t);
		}
		log.warn(getLogPrefix(session)+"validation failed: "+fullReasons, t);
		return event;
	}

     /**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.IPipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public abstract String validate(Object input, IPipeLineSession session, String logPrefix) throws XmlValidatorException;

    /**
     * Enable full schema grammar constraint checking, including
     * checking which may be time-consuming or memory intensive.
     *  Currently, particle unique attribution constraint checking and particle
     * derivation resriction checking are controlled by this option.
     * <p> see property http://apache.org/xml/features/validation/schema-full-checking</p>
     * Defaults to <code>false</code>;
     */
    public void setFullSchemaChecking(boolean fullSchemaChecking) {
        this.fullSchemaChecking = fullSchemaChecking;
        this.needsInit = true;
    }
	public boolean isFullSchemaChecking() {
		return fullSchemaChecking;
	}

    /**
     * <p>The filename of the schema on the classpath. The filename (which e.g.
     * can contain spaces) is translated to an URI with the
     * ClassUtils.getResourceURL(Object,String) method (e.g. spaces are translated to %20).
     * It is not possible to specify a namespace using this attribute.
     * <p>An example value would be "xml/xsd/GetPartyDetail.xsd"</p>
     * <p>The value of the schema attribute is only used if the schemaLocation
     * attribute and the noNamespaceSchemaLocation are not set</p>
     * @see ClassUtils.getResource(Object,String)
     */
    public void setSchema(String schema) {
        setNoNamespaceSchemaLocation(schema);
        this.needsInit = true;
    }
	public String getSchema() {
		return getNoNamespaceSchemaLocation();
	}

    /**
     * <p>Pairs of URI references (one for the namespace name, and one for a
     * hint as to the location of a schema document defining names for that
     * namespace name).</p>
     * <p> The syntax is the same as for schemaLocation attributes
     * in instance documents: e.g, "http://www.example.com file%20name.xsd".</p>
     * <p>The user can specify more than one XML Schema in the list.</p>
     * <p><b>Note</b> that spaces are considered separators for this attributed.
     * This means that, for example, spaces in filenames should be escaped to %20.
     * </p>
     *
     * N.B. since 4.3.0 schema locations are resolved automatically, without the need for ${baseResourceURL}
     */
    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        this.needsInit = true;
    }
	public String getSchemaLocation() {
		return schemaLocation;
	}

    /**
     * <p>A URI reference as a hint as to the location of a schema document with
     * no target namespace.</p>
     */
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
        this.needsInit = true;
    }
	public String getNoNamespaceSchemaLocation() {
		return noNamespaceSchemaLocation;
	}

	/**
	 * <p>The sessionkey to a value that is the uri to the schema definition.</P>
	 */
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}
	public String getSchemaSessionKey() {
		return schemaSessionKey;
	}

	/**
	 * @deprecated attribute name changed to {@link #setSchemaSessionKey(String) schemaSessionKey}
	 */
	public void setSchemaSession(String schemaSessionKey) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "attribute 'schemaSession' is deprecated. Please use 'schemaSessionKey' instead.";
		configWarnings.add(log, msg);
		this.schemaSessionKey = schemaSessionKey;
        this.needsInit = true;
	}

	protected String getLogPrefix(IPipeLineSession session){
		  StringBuilder sb = new StringBuilder();
		  sb.append(ClassUtils.nameOf(this)).append(' ');
		  if (this instanceof INamedObject) {
			  sb.append("[").append(((INamedObject)this).getName()).append("] ");
		  }
		  if (session != null) {
			  sb.append("msgId [").append(session.getMessageId()).append("] ");
		  }
		  return sb.toString();
	}

    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant.
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
	public boolean isThrowException() {
		return throwException;
	}

	/**
	 * The sessionkey to store the reasons of misvalidation in.
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}
	public String getReasonSessionKey() {
		return reasonSessionKey;
	}

	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		this.xmlReasonSessionKey = xmlReasonSessionKey;
	}
	public String getXmlReasonSessionKey() {
		return xmlReasonSessionKey;
	}

	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

	public void setValidateFile(boolean b) {
		validateFile = b;
	}
	public boolean isValidateFile() {
		return validateFile;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}
    protected InputSource getInputSource(Object input) throws XmlValidatorException {
        Variant in = new Variant(input);
        final InputSource is;
        if (isValidateFile()) {
            try {
                is = new InputSource(new InputStreamReader(new FileInputStream(in.asString()), getCharset()));
            } catch (FileNotFoundException e) {
                throw new XmlValidatorException("could not find file [" + in.asString() + "]", e);
            } catch (UnsupportedEncodingException e) {
                throw new XmlValidatorException("could not use charset [" + getCharset() + "]", e);
            }
        } else {
            is = in.asXmlInputSource();
        }
        return is;

    }

    protected static class RetryException extends XNIException {
        public RetryException(String s) {
            super(s);
        }
    }

    public static class UnknownNamespaceException extends RuntimeException {
        public UnknownNamespaceException(String s) {
            super(s);
        }
    }

    protected static class MyErrorHandler implements XMLErrorHandler {
        protected Logger log = LogUtil.getLogger(this);
        protected boolean throwOnError = false;
        protected boolean warn = true;

        public void warning(String domain, String key, XMLParseException e) throws XNIException {
            if (warn) ConfigurationWarnings.getInstance().add(log, e.getMessage());
        }

        public void error(String domain, String key, XMLParseException e) throws XNIException {
            if (throwOnError) {
                throw new RetryException(e.getMessage());
            }
            if (warn) ConfigurationWarnings.getInstance().add(log, e.getMessage());
        }

        public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
            if (warn) ConfigurationWarnings.getInstance().add(log, e.getMessage());
            throw new XNIException(e.getMessage());
        }
    }
}