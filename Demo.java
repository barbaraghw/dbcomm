package DBC;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo {
    public static void main(String[] args) {
        Properties properties = loadProperties("config.properties");

        if (properties == null) {
            System.err.println("No se pudo cargar el archivo de configuración.");
            return;
        }

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String pass = properties.getProperty("db.password");

        String selectQuery = properties.getProperty("db.selectQuery");

        // Crear instancia del pool de conexiones
        DBComponent connectionPool = new DBComponent(url, user, pass);
        connectionPool.start(); // Inicia el hilo del pool de conexiones

        // Ejemplo de ejecución concurrente utilizando ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(5); // Crea un pool de 5 threads

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    Connection connection = connectionPool.getConnection();
                    // Utilizar la conexión para ejecutar consultas o actualizaciones SQL
                    if (connection != null) {
                        // Por ejemplo, podrías ejecutar las consultas aquí usando PreparedStatement
                        // Ejemplo de ejecución de consulta select
                        executeQuery(connection, selectQuery);
                        
                    }

                    // Liberar la conexión al pool
                    connectionPool.releaseConnection(connection);
                } catch (SQLException e) {
                    System.err.println("Error al obtener o liberar conexión: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
    }

    private static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(fileName)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error cargando archivo de configuración: " + e.getMessage());
            return null;
        }
        return properties;
    }

    private static void executeQuery(Connection connection, String query) {
        // Implementa la lógica para ejecutar una consulta select
    }


}
