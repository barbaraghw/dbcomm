package DBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DBComponent extends Thread {
    private static final int INITIAL_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final int MAX_ADDITIONAL_CONNECTIONS = 10;
    private static final int MAX_CONNECTION_NUMBER = 30;

    private int maxPoolSize = INITIAL_POOL_SIZE;
    private int additionalConnections = 0;
    private int totalConnections = 0;
    private int connectionCounter = 1;

    private String url;
    private String user;
    private String pass;

    private final BlockingQueue<Connection> pool;

    public DBComponent(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.pool = new ArrayBlockingQueue<>(maxPoolSize);

        // Initialize the pool with minimum connections
        for (int i = 0; i < MIN_POOL_SIZE; i++) {
            try {
                Connection connection = DriverManager.getConnection(url, user, pass);
                pool.offer(connection);
                totalConnections++;
                printTotalConnections();
            } catch (SQLException e) {
                System.err.println("Error initializing the connection pool: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    while (pool.size() >= maxPoolSize) {
                        if (additionalConnections < MAX_ADDITIONAL_CONNECTIONS && maxPoolSize < MAX_POOL_SIZE) {
                            maxPoolSize += 2; // Increment pool size by 2 if within limits
                            additionalConnections += 2;
                            System.out.println("Increased pool size to: " + maxPoolSize);
                        } else {
                            wait();
                        }
                    }
                    Connection connection = createConnection();
                    if (connection != null) {
                        pool.offer(connection);
                        totalConnections++;
                        printTotalConnections();
                    }
                    notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Connection createConnection() {
        try {
            Connection connection = DriverManager.getConnection(url, user, pass);
            int currentConnectionNumber = connectionCounter;
            connectionCounter += 2;
            if (connectionCounter > MAX_CONNECTION_NUMBER) {
                connectionCounter = 2; // Reset counter if exceeds maximum
            }
            System.out.println("Connection created: " + currentConnectionNumber);
            return connection;
        } catch (SQLException e) {
            System.err.println("Error creating connection: " + e.getMessage());
            return null;
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        while (pool.isEmpty()) {
            if (pool.size() < maxPoolSize) {
                Connection connection = createConnection();
                if (connection != null) {
                    pool.offer(connection);
                    totalConnections++;
                    printTotalConnections();
                }
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        notifyAll();
        return pool.poll();
    }

    public synchronized void releaseConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                pool.offer(connection);
                notifyAll();
            } else {
                totalConnections--;
                printTotalConnections();
                if (totalConnections < maxPoolSize - additionalConnections) {
                    notifyAll();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public synchronized void closePool() throws SQLException {
        for (Connection connection : pool) {
            connection.close();
        }
        pool.clear();
        totalConnections = 0;
    }

    private void printTotalConnections() {
        System.out.println("Total connections: " + totalConnections);
    }
}
