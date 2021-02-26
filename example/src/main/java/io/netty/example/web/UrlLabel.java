import io.netty.handler.codec.http.HttpMethod;

import java.util.Objects;

// 定义处理借口
public interface Handlable {
    void handle(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response);
}

public class UrlLabel {
    private String uri;

    private HttpMethod method;
  
    // get setter 方法
    
  // 系统推荐要实现的，如果是要比较相等
  @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UrlLabel httpLabel = (UrlLabel) obj;
        return Objects.equals(uri, httpLabel.uri) && Objects.equals(method, httpLabel.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, method);
    }
  
  public class HttpRouters {
  private static Map<UrlLabel, HttpHandlable> routers = new HashMap<>();

    // 空构造函数, 单例访问
    private tHttpRouters() {
    }
    
    public static tHttpRouters getInstance() {
        return defaultHttpRouters;
    }
    
    public void register(HttpMethod httpMethod, String url, HttpHandlable handle) {
        register(new UrlLabel(url, httpMethod), handle);
    }
    
    public void register(UrlLabel label, HttpHandlable handle) {
        if (routers.containsKey(label)) {
            return;
        }
        routers.put(label, handle);
    }
  
  }
