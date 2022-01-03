package es.vn.operator;

public class K8sNotificationsDatabase {

	private String dbKind;
	private String dbUrl;
	private String dbUsername;
	private String dbPassword;

	public String getDbKind() {
		return dbKind;
	}

	public void setDbKind(String dbKind) {
		this.dbKind = dbKind;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public String getDbUsername() {
		return dbUsername;
	}

	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

}
