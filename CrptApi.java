import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Semaphore requestSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.requestSemaphore = new Semaphore(requestLimit);
        scheduleRequestLimitReset(timeUnit);
    }

    public void createDocument(CrptDocument document, String signature) {
        try {
            requestSemaphore.acquire(); // Acquire a permit, blocking if necessary
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = objectMapper.writeValueAsString(document);
            headers.add("Signature", signature);

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForObject(apiUrl, request, String.class);
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions appropriately
        } finally {
            requestSemaphore.release(); // Release the permit
        }
    }

    private void scheduleRequestLimitReset(TimeUnit timeUnit) {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1)); // Sleep for 1 time unit
                    requestSemaphore.release(requestSemaphore.availablePermits()); // Reset the permits
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
