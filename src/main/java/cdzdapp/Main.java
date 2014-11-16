package cdzdapp;

import cdzdapp.util.Config;
import cdzdapp.util.Database;
import cdzdapp.web.FriendsServlet;
import cdzdapp.web.LoginServlet;
import cdzdapp.web.LogoutServlet;
import com.ctlok.web.session.StatelessSessionFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

public class Main {
    public static final int HTTP_SERVER_PORT = Config.INSTANCE.getHttpPort();

    private static Server httpServer;
    private static Server shutdownServer;

    public static void main(String[] args) {
        start();
        try {
            httpServer.join();
        } catch (Exception e) {
            System.out.println("Unable to start server !");
            e.printStackTrace();
        }
    }

    public static void start() {
        Database.INSTANCE.migrate();

        createHttpServer();
        createShutdownServer();

        try {
            shutdownServer.start();
            httpServer.start();
        } catch (Exception e) {
            System.out.println("Unable to start server !");
            e.printStackTrace();
        }
    }

    public static void stop() {
        new Thread() {
            @Override
            public void run() {
                try {
                    shutdownServer.stop();
                    httpServer.stop();
                } catch (Exception e) {
                    System.out.println("Unable to shutdown server !");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void createHttpServer() {
        httpServer = new Server(HTTP_SERVER_PORT);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setBaseResource(Resource.newClassPathResource("web"));

        FilterHolder sessionFilter =
                webAppContext.addFilter(StatelessSessionFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        sessionFilter.setInitParameter("HMAC_SHA1_KEY", "top-s3cr3t-k3y");

        // Enable to encrypt the session cookie as well as signing it
        // sessionFilter.setInitParameter("ENCRYPTION_SECRET_KEY", "super-s3cr3t-k3y");

        webAppContext.addServlet(LoginServlet.class.getName(), "/");
        webAppContext.addServlet(FriendsServlet.class.getName(), "/friends");
        webAppContext.addServlet(LogoutServlet.class.getName(), "/logout");
        webAppContext.setConfigurations(new Configuration[]{new WebXmlConfiguration()});

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{webAppContext, new DefaultHandler()});
        httpServer.setHandler(handlers);
    }

    private static void createShutdownServer() {
        shutdownServer = new Server(Config.INSTANCE.getShutdownPort());
        shutdownServer.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if ("GET".equals(request.getMethod())) {
                    try {
                        Main.stop();
                    } catch (Exception e) {
                        System.out.println("Unable to shutdown server !");
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}