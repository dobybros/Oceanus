package script.core.servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServerServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        if(requestURI != null && requestURI.equals("/alive")) {
            response.setContentType("text/html");
//            long time = System.currentTimeMillis();
//            double d = 0.0f;
//            for(int i = 0; i < 100000000; i++) {
//                d = 1000 * 0.02;
//            }
//            try {
//                response.getOutputStream().write(("<html><body>alive " + (System.currentTimeMillis() - time) + " </body></html>").getBytes(StandardCharsets.UTF_8));
//            } catch (IOException e) {
//                   e.printStackTrace();
//            }
            response.setStatus(200);
        }
    }

}
