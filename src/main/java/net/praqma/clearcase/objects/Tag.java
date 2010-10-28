package net.praqma.clearcase.objects;

public class Tag {
	private String type; //eg buildInProgress
	private String hudsonJob;
	private String timestamp;
	
	public Tag(String type, String hudsonJob, String timestamp){
		this.type = type;
		this.hudsonJob = hudsonJob;
		this.timestamp = timestamp;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setHudsonJob(String hudsonJob) {
		this.hudsonJob = hudsonJob;
	}

	public String getHudsonJob() {
		return hudsonJob;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getTimestamp() {
		return timestamp;
	}

}
