/*
 * $Log: TibcoConnection.java,v $
 * Revision 1.2  2008-05-15 14:53:12  europe\L190409
 * remove unnecessary overridden code
 *
 * Revision 1.1  2008/05/15 14:32:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.extensions.tibco;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.JmsConnection;
import nl.nn.adapterframework.jms.JmsException;

import com.tibco.tibjms.TibjmsConnectionFactory;

/**
 * Wrapper around Tibco connection objects.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class TibcoConnection extends JmsConnection {
	public static final String version="$RCSfile: TibcoConnection.java,v $ $Revision: 1.2 $ $Date: 2008-05-15 14:53:12 $";
	
	private TibjmsConnectionFactory connectionFactory;
	
	public TibcoConnection(String connectionFactoryName, Context context, ConnectionFactory connectionFactory, Map connectionMap) {
		super(connectionFactoryName, context, connectionFactory, connectionMap);
		this.connectionFactory=(TibjmsConnectionFactory)connectionFactory;
	}

	protected Connection createConnection() throws JMSException {
		String userName=null;
		String password=null;
		return connectionFactory.createConnection(userName,password);
	}


	public Destination lookupDestination(String destinationName) throws JmsException {
		Session session=null;		
		try {
			session = createSession(false,Session.AUTO_ACKNOWLEDGE);
			log.debug("Session class ["+session.getClass().getName()+"]");
			Destination destination;

			/* create the destination */
			if (session instanceof TopicSession) {
				destination = ((TopicSession)session).createTopic(destinationName);
			} else {
				destination = ((QueueSession)session).createQueue(destinationName);
			}

			return destination;
		} catch (Exception e) {
			throw new JmsException("cannot create destination", e);
		} finally {
			releaseSession(session);
		}
	}
	
}