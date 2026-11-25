package eu.openaire.catalogue.dsm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsmDatasourceControllerTest {

    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;

    @AfterEach
    void stopServer() {
        release.countDown();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void theHeaderGivesThePage() {
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", Map.of("page", "2", "size", "10", "total", "25"),
                "datasourceInfo", List.of("a", "b", "c", "d", "e")));

        assertEquals(20, paging.getFrom());
        assertEquals(24, paging.getTo());
        assertEquals(25, paging.getTotal());
        assertEquals(5, paging.getResults().size());
    }

    @Test
    void missingOrInvalidHeaderFieldsFallBackToTheResults() {
        Map<String, Object> header = new HashMap<>();
        header.put("page", null);
        header.put("size", "many");
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", header, "datasourceInfo", List.of("a", "b")));

        assertEquals(0, paging.getFrom());
        assertEquals(1, paging.getTo());
        assertEquals(2, paging.getTotal());
    }

    @Test
    void anEmptyPageEndsBeforeItStarts() {
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", Map.of("page", 0, "size", 10, "total", 0), "datasourceInfo", List.of()));

        assertEquals(0, paging.getFrom());
        assertEquals(-1, paging.getTo());
        assertEquals(0, paging.getTotal());
    }

    @Test
    void aPageIsReadFromTheApi() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        DsmDatasourceController controller = controller(exchange -> {
            path.set(exchange.getRequestURI().getPath());
            respond(exchange, 200, "application/json",
                    "{\"header\":{\"page\":1,\"size\":10,\"total\":12},\"datasourceInfo\":[{\"id\":\"a\"},{\"id\":\"b\"}]}");
        });

        Paging<Object> paging = controller.search(Map.of("from", "10", "quantity", "10"));

        assertEquals("/ds/searchregistered/1/10/", path.get());
        assertEquals(12, paging.getTotal());
        assertEquals(List.of(Map.of("id", "a"), Map.of("id", "b")), paging.getResults());
    }

    @Test
    void anErrorPageOfTheApiIsNotPassedOn() throws IOException {
        DsmDatasourceController controller = controller(exchange ->
                respond(exchange, 504, "text/html", "<html><body>504 Gateway Time-out</body></html>"));

        ResourceException e = assertThrows(ResourceException.class, () -> controller.search(Map.of()));

        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals("The OpenAIRE datasource API failed", e.getMessage());
        assertInstanceOf(HttpServerErrorException.class, e.getCause());
    }

    @Test
    @Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aStalledApiTimesOut() throws IOException {
        DsmDatasourceController controller = controller(Duration.ofMillis(300), exchange -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });

        for (Executable call : List.<Executable>of(() -> controller.search(Map.of()), () -> controller.get("id"))) {
            ResourceException e = assertThrows(ResourceException.class, call);
            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
            assertInstanceOf(ResourceAccessException.class, e.getCause());
        }
    }

    @Test
    @Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void anUnavailableApiIsNotRetried() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        DsmDatasourceController controller = controller(exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().set("Retry-After", "60");
            respond(exchange, 503, "text/html", "unavailable");
        });

        assertThrows(ResourceException.class, () -> controller.search(Map.of()));
        assertThrows(ResourceException.class, () -> controller.get("id"));

        assertEquals(2, requests.get());
    }

    @Test
    @Timeout(value = 20, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aConnectionTheApiResetsIsABadGateway() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            CountDownLatch reset = new CountDownLatch(1);
            Thread.ofPlatform().daemon().start(() -> {
                try (Socket socket = listener.accept()) {
                    socket.getInputStream().read(new byte[8192]);
                    Thread.sleep(100);
                    String body = "{}";
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"
                            + "Content-Length: " + body.length() + "\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8));
                    Thread.sleep(100);
                    socket.setSoLinger(true, 0);
                } catch (IOException | InterruptedException e) {
                    throw new IllegalStateException(e);
                } finally {
                    reset.countDown();
                }
            });
            DsmDatasourceController controller = new DsmDatasourceController(
                    properties("http://localhost:" + listener.getLocalPort() + "/", Duration.ofSeconds(1)));
            assertEquals(List.of(), controller.search(Map.of()).getResults());
            reset.await();

            ResourceException e = assertThrows(ResourceException.class, () -> controller.search(Map.of()));

            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
            assertEquals("The OpenAIRE datasource API failed", e.getMessage());
        }
    }

    @Test
    void aNegativeOffsetIsABadRequest() {
        DsmDatasourceController controller = new DsmDatasourceController(new OpenAIREProperties());

        ResourceException e = assertThrows(ResourceException.class,
                () -> controller.search(Map.of("from", "-10", "quantity", "10")));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    private DsmDatasourceController controller(HttpHandler handler) throws IOException {
        return controller(new OpenAIREProperties().getDsm().getReadTimeout(), handler);
    }

    private DsmDatasourceController controller(Duration readTimeout, HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", handler);
        server.setExecutor(Executors.newCachedThreadPool(Thread.ofPlatform().daemon().factory()));
        server.start();
        String url = "http://localhost:" + server.getAddress().getPort() + "/";
        return new DsmDatasourceController(properties(url, readTimeout));
    }

    private static OpenAIREProperties properties(String url, Duration readTimeout) {
        OpenAIREProperties properties = new OpenAIREProperties();
        properties.getDsm().setUrl(url);
        properties.getDsm().setProvideUrl(url);
        properties.getDsm().setReadTimeout(readTimeout);
        return properties;
    }

    private static void respond(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
