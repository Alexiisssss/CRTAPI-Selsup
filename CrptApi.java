

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.rateLimiter = new RateLimiter(timeUnit, requestLimit);
    }

    public HttpResponse<String> createDocument(Document document, String signature) throws IOException, InterruptedException {
        rateLimiter.acquire();

        ObjectNode json = objectMapper.createObjectNode();
        json.set("description", objectMapper.valueToTree(document));
        json.put("signature", signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type = "LP_INTRODUCE_GOODS";
        public boolean importRequest = true;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
    }

    public static class Description {
        public String participantInn;
    }

    public static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
    }

    private static class RateLimiter {
        private final long intervalMillis;
        private final int maxRequests;
        private final Semaphore semaphore;
        private long nextAllowedTime;
        private final Object lock = new Object();

        public RateLimiter(TimeUnit timeUnit, int maxRequests) {
            this.intervalMillis = timeUnit.toMillis(1);
            this.maxRequests = maxRequests;
            this.semaphore = new Semaphore(maxRequests);
            this.nextAllowedTime = System.currentTimeMillis();
        }

        public void acquire() throws InterruptedException {
            synchronized (lock) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= nextAllowedTime) {
                    nextAllowedTime = currentTime + intervalMillis;
                    semaphore.drainPermits();
                    semaphore.release(maxRequests);
                }
            }
            semaphore.acquire();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        Document document = new Document();
        document.description = new Description();
        document.description.participantInn = "1234567890"; // Укажите ваш ИНН участника
        document.doc_id = "doc_id_example"; // Укажите уникальный идентификатор документа
        document.doc_status = "NEW"; // Статус документа, например, "NEW"
        document.owner_inn = "1234567890"; // Укажите ваш ИНН
        document.participant_inn = "1234567890"; // Укажите ИНН участника
        document.producer_inn = "1234567890"; // Укажите ИНН производителя
        document.production_date = "2024-07-24"; // Дата производства, например, "2024-07-24"
        document.production_type = "type_example"; // Укажите тип производства
        document.products = new Product[]{ new Product() };
        document.products[0].certificate_document = "cert_doc"; // Укажите номер сертификата
        document.products[0].certificate_document_date = "2024-07-24"; // Дата сертификата
        document.products[0].certificate_document_number = "cert_number"; // Укажите номер сертификата
        document.products[0].owner_inn = "1234567890"; // Укажите ваш ИНН
        document.products[0].producer_inn = "1234567890"; // Укажите ИНН производителя
        document.products[0].production_date = "2024-07-24"; // Дата производства
        document.products[0].tnved_code = "code"; // Укажите код ТН ВЭД
        document.products[0].uit_code = "uit_code"; // Укажите УИТ код
        document.products[0].uitu_code = "uitu_code"; // Укажите УИТУ код
        document.reg_date = "2024-07-24"; // Дата регистрации
        document.reg_number = "reg_number"; // Укажите номер регистрации


        HttpResponse<String> response = crptApi.createDocument(document, "signature_example");

        System.out.println("Response status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());
    }
}
