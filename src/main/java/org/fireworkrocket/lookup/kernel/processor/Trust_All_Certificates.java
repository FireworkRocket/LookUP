package org.fireworkrocket.lookup.kernel.processor;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

import static org.fireworkrocket.lookup.exception.ExceptionHandler.handleException;

/**
 * 该类实现了 X509TrustManager 和 HostnameVerifier 接口，用于信任所有 HTTPS 证书。
 */
public class Trust_All_Certificates implements X509TrustManager, HostnameVerifier {

    // 单例实例，使用懒加载
    private static class Holder {
        private static final Trust_All_Certificates INSTANCE = new Trust_All_Certificates();
    }

    // 私有构造函数，防止外部实例化
    private Trust_All_Certificates() {}

    /**
     * 获取单例实例
     * @return Trust_All_Certificates 实例
     */
    public static Trust_All_Certificates getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 返回接受的证书颁发机构
     * @return 空的 X509Certificate 数组
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    /**
     * 检查客户端证书
     * @param certs 客户端证书
     * @param authType 认证类型
     */
    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {}

    /**
     * 检查服务器证书
     * @param certs 服务器证书
     * @param authType 认证类型
     */
    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {}

    /**
     * 验证主机名
     * @param hostname 主机名
     * @param session SSL 会话
     * @return 始终返回 true，表示信任所有主机名
     */
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }

    /**
     * 信任所有 HTTPS 证书
     * @throws Exception 如果初始化 SSLContext 失败
     */
    public static void trustAllHttpsCertificates() throws Exception {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{getInstance()};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            handleException(e);
        }
    }
}