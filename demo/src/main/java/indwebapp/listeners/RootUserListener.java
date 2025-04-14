package indwebapp.listeners;

import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import indwebapp.util.RootUserInstance;

@WebListener
public class RootUserListener implements ServletContextListener {

    @Override
    public void contextInitialized(jakarta.servlet.ServletContextEvent sce) {
        // Initialize the root user instance
        RootUserInstance.getInstance();
    }

    @Override
    public void contextDestroyed(jakarta.servlet.ServletContextEvent sce) {
        // Cleanup if necessary
    }

}
