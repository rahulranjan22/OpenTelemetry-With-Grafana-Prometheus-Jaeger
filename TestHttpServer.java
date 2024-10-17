import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

public class TestHttpServer {
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("TestHttpServer");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
        server.createContext("/api/simulate", new ApiHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server is listening on port 8001");

        Span mainSpan = tracer.spanBuilder("Main Transaction").startSpan();
        try (Scope scope = mainSpan.makeCurrent()) {
            while (true) {
                simulateApiCall("GET /api/users");
                simulateApiCall("POST /api/orders");
                simulateApiCall("PUT /api/products/123");
                simulateApiCall("DELETE /api/items/456");
                simulateApiCall("GET /api/error");
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.out.println("Main thread interrupted: " + e.getMessage());
        } finally {
            mainSpan.end();
        }
    }

    private static void simulateApiCall(String apiEndpoint) {
        Span span = tracer.spanBuilder(apiEndpoint).startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("api.endpoint", apiEndpoint);
            if (apiEndpoint.contains("error")) {
                throw new CustomException("Simulated error for " + apiEndpoint, 1001);
            }
            simulateWork();
        } catch (CustomException e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Error occurred: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    private static void simulateWork() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            System.out.println("Work thread interrupted: " + e.getMessage());
        }
    }

    static class CustomException extends RuntimeException {
        private final int errorCode;

        public CustomException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String response = "{\"message\": \"Simulated API call successful\"}";
            if (query != null && query.contains("endpoint=")) {
                String apiEndpoint = query.split("endpoint=")[1];
                simulateApiCall(apiEndpoint);
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }
}
