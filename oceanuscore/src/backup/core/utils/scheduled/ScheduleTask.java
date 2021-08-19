package core.utils.scheduled;


import org.quartz.SchedulerException;

public abstract class ScheduleTask extends AbstractScheduleTask {
	private String id;
	private Long delay;
	private Long period;
	private String cron;
	private Long scheduleTime;
	private String description;
	public ScheduleTask(){
		this.description = "NOID_";
	}
	public ScheduleTask(String id){
		this.id = id;
	}
	public ScheduleTask(String id, String description){
		this.id = id;
		this.description = description;
	}
	@Override
	public abstract void execute();

	public void cancel() throws SchedulerException {
		if(this.id != null){
			QuartzFactory.getInstance().removeJob(this.id);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getDelay() {
		return delay;
	}

	public void setDelay(Long delay) {
		this.delay = delay;
	}

	public Long getPeriod() {
		return period;
	}

	public void setPeriod(Long period) {
		this.period = period;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public Long getScheduleTime() {
		return scheduleTime;
	}

	public void setScheduleTime(Long scheduleTime) {
		this.scheduleTime = scheduleTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
