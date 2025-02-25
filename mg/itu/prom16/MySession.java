package mg.itu.prom16;

import javax.servlet.http.HttpSession;

public class MySession {
    private HttpSession session;

    public MySession() {
    }

    public void add(String key, Object value) {
        session.setAttribute(key, value);
    }

    public void delete(String key) {
        session.removeAttribute(key);
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void setSession(HttpSession sess) {
        session = sess;
    }
}
