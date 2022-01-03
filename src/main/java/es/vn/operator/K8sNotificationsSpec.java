package es.vn.operator;

public class K8sNotificationsSpec {

	private String image;
	private String name;
	private String namespace;
	private String slackToken;
	private String slackChannelId;

	private K8sNotificationsDatabase database;
	
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSlackToken() {
		return slackToken;
	}

	public void setSlackToken(String slackToken) {
		this.slackToken = slackToken;
	}

	public String getSlackChannelId() {
		return slackChannelId;
	}

	public void setSlackChannelId(String slackChannelId) {
		this.slackChannelId = slackChannelId;
	}

	public K8sNotificationsDatabase getDatabase() {
		return database;
	}

	public void setDatabase(K8sNotificationsDatabase database) {
		this.database = database;
	}

}
