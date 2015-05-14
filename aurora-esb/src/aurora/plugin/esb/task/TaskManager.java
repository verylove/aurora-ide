package aurora.plugin.esb.task;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

import aurora.plugin.esb.AuroraEsbContext;
import aurora.plugin.esb.model.DirectConfig;
import aurora.plugin.esb.model.Router;
import aurora.plugin.esb.model.Task;
import aurora.plugin.esb.model.TaskStatus;
import aurora.plugin.esb.router.builder.DirectWSRouteBuilder;
import aurora.plugin.esb.router.builder.MonitorDirectRouteBuilder;

public class TaskManager {
	private CamelContext context;
	private String workPath = null;
	private AuroraEsbContext esbContext;

	public TaskManager(AuroraEsbContext esbContext) {
		super();
		this.context = esbContext.getCamelContext();
		this.workPath = esbContext.getWorkPath();
		this.esbContext = esbContext;
	}

	public Task createTask(DirectConfig config) {
		// timer task
		// direct task
		Task createTask = new Task();
		createTask.setName(config.getName());
		createTask.setRouter(config.getRouter());
		TaskStore s = new TaskStore(workPath);
		s.save(createTask);
		return createTask;
	}

	public void configDirectRouter(DirectConfig config) {
		// Task createTask = Demo.createTask();
		try {
//			context.addRoutes(new DirectWSRouteBuilder(config, esbContext));
			context.addRoutes(new MonitorDirectRouteBuilder(config, esbContext));
		} catch (Exception e) {
			e.printStackTrace();
			// this.updateTaskStatus(createTask, TaskStatus.ERROR_ON_STARTING);
		}
	}

	public void directStartTask(DirectConfig directConfig) {
		try {
			ProducerTemplate template = context.createProducerTemplate();
			Router createRouter = directConfig.getRouter();
			template.sendBody("direct:" + createRouter.getFrom().getName(),
					createRouter.getFrom().getParaText());
		} catch (Exception e) {
			e.printStackTrace();
			// this.updateTaskStatus(task, TaskStatus.ERROR_ON_STARTING);
		}
	}

	public void directStartTask(Task task) {
		try {
			ProducerTemplate template = context.createProducerTemplate();
			Router createRouter = task.getRouter();
			template.sendBody("direct:" + createRouter.getFrom().getName(),
					createRouter.getFrom().getParaText());
		} catch (Exception e) {
			e.printStackTrace();
			this.updateTaskStatus(task, TaskStatus.ERROR_ON_STARTING);
		}
	}

	public void finishTask(Task task) {
		updateTaskStatus(task, TaskStatus.FINISH);
	}

	public void stopTask(Task task) {
		updateTaskStatus(task, TaskStatus.STOPED);
	}

	public void updateTaskStatus(Task task, String status) {
		task.setStatus(status);
		updateTask(task);
	}

	public void updateTask(Task task) {
		new TaskStore(workPath).update(task);
	}

	public Task updateTaskStatus(String task_id, String task_name, String status) {
		Task t = loadTask(task_id, task_name);
		updateTaskStatus(t, status);
		return t;
	}

	private Task loadTask(String task_id, String task_name) {
		return new TaskStore(workPath).getTask(task_id, task_name);
	}

}