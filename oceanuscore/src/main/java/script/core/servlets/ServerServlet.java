package script.core.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServerServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        if(requestURI != null && requestURI.equals("/alive")) {
            response.setContentType("text/html");
            try {
                response.getOutputStream().write("<html><body>alive</body></html>".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                   e.printStackTrace();
            }
            response.setStatus(200);
        }
    }

}
