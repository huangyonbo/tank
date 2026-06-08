package framework.game;

public enum MailSystemDef {
	MAIL_NORMAL, //
	MAIL_BACK, // 后台
	MAIL_ACTIVITY, // 活动
	MAIL_RANK, // 排行榜
	MAIL_MASTER, // master命令
	MAIL_GUILD, // 公会邮件
	END;

	public static MailSystemDef getSystem(int ordinal) {
		for (MailSystemDef systemDef : values()) {
			if (systemDef.ordinal() == ordinal) {
				return systemDef;
			}
		}
		return END;
	}
}
