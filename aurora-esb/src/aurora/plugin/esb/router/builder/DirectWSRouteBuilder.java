package aurora.plugin.esb.router.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.util.Base64;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import aurora.plugin.esb.AuroraEsbContext;
import aurora.plugin.esb.console.ConsoleLog;
import aurora.plugin.esb.model.AMQMsg;
import aurora.plugin.esb.model.DirectConfig;
import aurora.plugin.esb.model.From;
import aurora.plugin.esb.model.Router;
import aurora.plugin.esb.model.TO;
import aurora.plugin.esb.model.Task;
import aurora.plugin.esb.model.TaskStatus;
import aurora.plugin.esb.model.XMLHelper;
import aurora.plugin.esb.task.TaskManager;

public class DirectWSRouteBuilder extends RouteBuilder {

	private Router r;
	private DirectConfig config;
	private CamelContext context;
	private ConsoleLog clog = new ConsoleLog();
	private String workPath;
	private AuroraEsbContext esbContext;
//	static private String path = "/Users/shiliyan/Desktop/esb/";

	// get server data
	// save to file
	// send msg to amq

	// get amq msg
	// get local data

	// send data to client
	// get response
	// save to file
	// send msg to amq

	// feedback to server

	public DirectWSRouteBuilder(DirectConfig config, AuroraEsbContext esbContext) {
		this.config = config;
		this.r = config.getRouter();
		this.context = esbContext.getCamelContext();
		 workPath = esbContext.getWorkPath();
		 this.esbContext = esbContext;
	}

	private String loadPara(Task task, From from) {
		try {
			String exchangeID = from.getExchangeID();
			String name = from.getName();

			return loadSavedParaData(task, exchangeID, name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String loadSavedParaData(Task task, String exchangeID, String name)
			throws FileNotFoundException, IOException {
		int lastIndexOf = exchangeID.lastIndexOf("-");
		String e = exchangeID.substring(lastIndexOf);
		String fileid = exchangeID.substring(0, lastIndexOf)
				+ (Integer.valueOf(e) - 1);

		File file = new File(workPath + task.getRouter().getName() + "/" + name,
				fileid);
		// from.getExchangeID()
		FileInputStream fis = new FileInputStream(file);
		String inputStream2String = XMLHelper.inputStream2String(fis);
		return inputStream2String;
	}

	private void msgBuilder() {
		TO to = r.getTo();
		from("test-jms:get_data_record").process(new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				String body = exchange.getIn().getBody(String.class);
				AMQMsg object = AMQMsg.toObject(body);
				Task task = object.getTask();
				exchange.getOut().setHeader("task_id", task.getId());
				exchange.getOut().setHeader("task_name", task.getName());

				exchange.getOut().setBody(
						loadPara(task, task.getRouter().getFrom()));
			}

		}).to("direct:" + to.getName());
		From from = r.getFrom();
		if (from.getFeedbackPoint() != null
				&& "".equals(from.getFeedbackPoint().trim()) == false) {
			from("test-jms:send_data_record").process(new Processor() {

				@Override
				public void process(Exchange arg0) throws Exception {
					Map<String, Object> paras = wsHeaderOptions(
							from.getUserName(), from.getPsd());

					String body = arg0.getIn().getBody(String.class);
					AMQMsg object = AMQMsg.toObject(body);
					Task task = object.getTask();

					paras.put("task_id", task.getId());
					paras.put("task_name", task.getName());

					arg0.getOut().setHeaders(paras);

					arg0.getOut().setBody(
							loadPara(task, task.getRouter().getTo()));

					updateTaskStatus(arg0, TaskStatus.FEEDBACKED);
					clog.log2Console(arg0, TaskStatus.FEEDBACKED);
				}
			}).to(from.getFeedbackPoint());
			// .process(new Processor() {
			//
			// @Override
			// public void process(Exchange exchange) throws Exception {
			// }
			// });
		}
	}

	private String loadPara(Task task, TO to) {

		try {
			String exchangeID = to.getExchangeID();
			String name = to.getName();
			return loadSavedParaData(task, exchangeID, name);
			// int lastIndexOf = exchangeID.lastIndexOf("-");
			// String e = exchangeID.substring(lastIndexOf);
			// String fileid = exchangeID.substring(0, lastIndexOf)
			// + (Integer.valueOf(e) - 1);
			//
			// File file = new File(path + task.getRouter().getName() + "/"
			// + name, fileid);
			//
			// FileInputStream fis = new FileInputStream(file);
			// String inputStream2String = XMLHelper.inputStream2String(fis);
			// return inputStream2String;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	

	private void serverBuilder() {
		From from = r.getFrom();
		from("direct:" + from.getName())
				.process(new Processor() {

					@Override
					public void process(Exchange arg0) throws Exception {
						Map<String, Object> paras = wsHeaderOptions(
								from.getUserName(), from.getPsd());
						TaskManager tm = new TaskManager(esbContext);
						Task t = tm.createTask(config);

						t.getRouter().getFrom()
								.setExchangeID(arg0.getExchangeId());

						tm.updateTask(t);
						paras.put("task_id", t.getId());
						paras.put("task_name", t.getName());
						arg0.getOut().setHeaders(paras);
						arg0.getOut().setBody(arg0.getIn().getBody());
						arg0.getIn().setHeader("task_id", t.getId());
						arg0.getIn().setHeader("task_name", t.getName());
						clog.log2Console(arg0, t.getStatus());
					}
				})
				.to("" + from.getEndpoint())
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {

						keepTaskAlive(exchange);
						Task task = updateTaskStatus(exchange,
								TaskStatus.SERVER_DATA_LOADED);
						exchange.getOut().setBody(exchange.getIn().getBody());
						clog.log2Console(exchange,
								TaskStatus.SERVER_DATA_LOADED);
					}
				}).to("file:" + workPath + r.getName() + "/" + from.getName())
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {

						keepTaskAlive(exchange);
						Task task = updateTaskStatus(exchange,
								TaskStatus.SERVER_WORK_FINISH);

						AMQMsg msg = new AMQMsg();
						msg.setTask(task);
						exchange.getOut().setBody(AMQMsg.toXML(msg));
						clog.log2Console(exchange,
								TaskStatus.SERVER_WORK_FINISH);
					}
				}).to("test-jms:get_data_record");
	}

	private void keepTaskAlive(Exchange exchange) {

		String task_id = (String) exchange.getIn().getHeader("task_id");
		String task_name = (String) exchange.getIn().getHeader("task_name");
		exchange.getOut().setHeader("task_id", task_id);
		exchange.getOut().setHeader("task_name", task_name);
	}

	private Task updateTaskStatus(Exchange exchange, String status) {
		String task_id = (String) exchange.getIn().getHeader("task_id");
		String task_name = (String) exchange.getIn().getHeader("task_name");
		TaskManager tm = new TaskManager(esbContext);
		Task task = tm.updateTaskStatus(task_id, task_name, status);
		return task;
	}

	private void clientBuilder() {
		TO to = r.getTo();
		from("direct:" + to.getName())
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						Map<String, Object> paras = wsHeaderOptions(
								to.getUserName(), to.getPsd());
						String task_id = (String) exchange.getIn().getHeader(
								"task_id");
						String task_name = (String) exchange.getIn().getHeader(
								"task_name");
						paras.put("task_id", task_id);
						paras.put("task_name", task_name);

						exchange.getOut().setHeaders(paras);
						exchange.getOut().setBody(exchange.getIn().getBody());
						Task task = updateTaskStatus(exchange,
								TaskStatus.INVOKE_CLIENT_POINT);
						task.getRouter().getTo()
								.setExchangeID(exchange.getExchangeId());
						TaskManager tm = new TaskManager(esbContext);
						tm.updateTask(task);
						clog.log2Console(exchange,
								TaskStatus.INVOKE_CLIENT_POINT);
					}
				}).to("" + to.getEndpoint()).process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						keepTaskAlive(exchange);
						updateTaskStatus(exchange, TaskStatus.CLIENT_RESPONSED);
						exchange.getOut().setBody(exchange.getIn().getBody());
						clog.log2Console(exchange, TaskStatus.CLIENT_RESPONSED);
					}
				}).to("file:" + workPath + r.getName() + "/" + to.getName())
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						keepTaskAlive(exchange);
						Task task = updateTaskStatus(exchange,
								TaskStatus.CLIENT_RESPONSED_SAVED);

						AMQMsg msg = new AMQMsg();
						msg.setTask(task);
						exchange.getOut().setBody(AMQMsg.toXML(msg));
						clog.log2Console(exchange,
								TaskStatus.CLIENT_RESPONSED_SAVED);
					}
				}).to("test-jms:send_data_record").process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						Thread.sleep(1000);
						Task task = updateTaskStatus(exchange,
								TaskStatus.FINISH);
						clog.log2Console(exchange, TaskStatus.FINISH);
					}

				});
	}

	@Override
	public void configure() throws Exception {
		serverBuilder();
		msgBuilder();
		clientBuilder();
	}

	private Map<String, Object> wsHeaderOptions(String userName, String psd) {

		Map<String, Object> paras = new HashMap<String, Object>();
		paras.put("soapaction", "urn:anonOutInOp");
		paras.put("Content-Type", "text/xml; charset=UTF-8");

		String encoded = new String(Base64.encode(new String(userName + ":"
				+ psd).getBytes()));
		paras.put("Authorization", "Basic " + encoded);
		return paras;
	}

}