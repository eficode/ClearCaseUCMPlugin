package net.praqma.clearcase.objects;

public class Tag
{
	private String key;
	private String value;
	private String hudsonJob;
	private String timestamp;
	
	public Tag( String key, String hudsonJob, String timestamp )
	{
		this.key       = key;
		this.value     = "true";
		this.hudsonJob = hudsonJob;
		this.timestamp = timestamp;
	}
	
	public Tag( String key, String value, String hudsonJob, String timestamp )
	{
		this.key       = key;
		this.value     = value;
		this.hudsonJob = hudsonJob;
		this.timestamp = timestamp;
	}
	
	public void SetKey( String key )
	{
		this.key = key;
	}

	public String GetKey()
	{
		return key;
	}
	
	public void SetValue( String value )
	{
		this.value = value;
	}

	public String GetValue()
	{
		return value;
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
