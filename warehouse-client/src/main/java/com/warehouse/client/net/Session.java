package com.warehouse.client.net;

/** 当前会话：登录用户信息 + 服务器地址。
 *  客户端中**绝不保存**数据库账号密码。 */
public class Session {
    private static final Session I = new Session();
    private Session() {}

    public static Session get() { return I; }

    private String serverUrl = "http://localhost:8080/warehouse";
    private String userCode;
    private String realName;
    private String address;

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String s) { this.serverUrl = s; }

    public String getUserCode() { return userCode; }
    public String getRealName() { return realName; }
    public String getAddress()  { return address; }

    public void setUser(String userCode, String realName, String address) {
        this.userCode = userCode;
        this.realName = realName;
        this.address  = address;
    }
}
