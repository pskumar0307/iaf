/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.senders.IbisLocalSender;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Job, generated by SchedulerSender, for sending messages to a javalistener.
 * The <a href="http://quartz.sourceforge.net">Quartz scheduler</a> is used for scheduling.
 * <p>
 * Job is registered at runtime by the SchedulerSender
 * 
 * @author John Dekker
 */
public class ServiceJob extends BaseJob {
	public static final String version="$RCSfile: ServiceJob.java,v $ $Revision: 1.5 $ $Date: 2011-11-30 13:51:42 $";

	public ServiceJob() {
		super();
	}
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			log.info("executing" + getLogPrefix(context));
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			String serviceName = dataMap.getString(SchedulerSender.JAVALISTENER);
			String message = dataMap.getString(SchedulerSender.MESSAGE);
			String correlationId = dataMap.getString(SchedulerSender.CORRELATIONID);
			
			// send job
			IbisLocalSender localSender = new IbisLocalSender();
			localSender.setJavaListener(serviceName);
			localSender.setIsolated(false);
			localSender.setName("ServiceJob");
			localSender.configure();
			
			localSender.open();
			try {
				localSender.sendMessage(correlationId, message);
			}
			finally {
				localSender.close();
			}
		}
		catch (Exception e) {
			log.error(e);
			throw new JobExecutionException(e, false);
		}
		log.debug(getLogPrefix(context) + "completed");
	}

}