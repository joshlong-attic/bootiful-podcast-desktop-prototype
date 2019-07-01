package fm.bootifulpodcast.desktop.client;

import fm.bootifulpodcast.desktop.StageReadyEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.SocketException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class ApiClient {

	private final AtomicBoolean connected = new AtomicBoolean(false);

	private final ScheduledExecutorService executor;

	private final RestTemplate restTemplate;

	private final ApplicationEventPublisher publisher;

	private final String serverUrl, actuatorUrl;

	private final int monitorDelayInSeconds;

	public ApiClient(String serverUrl, ScheduledExecutorService executor,
																		ApplicationEventPublisher publisher, RestTemplate restTemplate,
																		int interval) {

		this.monitorDelayInSeconds = interval;
		this.executor = executor;
		this.restTemplate = restTemplate;
		this.publisher = publisher;

		Assert.hasText(serverUrl, "the server URL provided is null");
		this.serverUrl = serverUrl.endsWith("/")
			? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
		this.actuatorUrl = this.serverUrl + "/actuator/health";

		log.debug("the server URL is " + this.serverUrl + " and the actuator URL is "
			+ this.actuatorUrl);

	}

	/*
			we register this monitor thread *after* other controllers
 		are active and able to receive events. can't launch this in constructor because
 		it'll be connected *before* the UI components.
	 */
	@EventListener(StageReadyEvent.class)
	public void stageIsReady() {
		this.executor.scheduleWithFixedDelay(this::monitorConnectedEndpoint, 0,
			this.monitorDelayInSeconds, TimeUnit.SECONDS);
	}

	private void monitorConnectedEndpoint() {
		try {
			var typeReference = new ParameterizedTypeReference<Map<String, Object>>() {
			};
			var entity = this.restTemplate.exchange(this.actuatorUrl, HttpMethod.GET,
				HttpEntity.EMPTY, typeReference);
			var body = entity.getBody();
			var jsonMap = Objects.requireNonNull(body);
			var status = (String) jsonMap.get("status");
			var isActuatorHealthy = entity.getStatusCode().is2xxSuccessful() && status.equalsIgnoreCase("UP");
			if (isActuatorHealthy && this.connected.compareAndSet(false, true)) {
				publisher.publishEvent(new ApiConnectedEvent());
			}
		}
		catch (Exception e) {
			if (e instanceof ResourceAccessException || e instanceof SocketException) {
				log.debug("couldn't connect to " + this.actuatorUrl);
			}
			if (this.connected.compareAndSet(true, false)) {
				publisher.publishEvent(new ApiDisconnectedEvent());
			}
		}
	}

	public ProductionStatus beginProduction(String uid, File archive) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		var resource = new FileSystemResource(archive);
		var body = new LinkedMultiValueMap<String, Object>();
		body.add("file", resource);
		var requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);
		var url = this.serverUrl + "/podcasts/" + uid;
		var response = restTemplate.postForEntity(url, requestEntity, String.class);
		var good = response.getStatusCode().is2xxSuccessful();
		var location = response.getHeaders().getLocation();
		Assert.notNull(location, "the location URI must be non-null");
		return new ProductionStatus(URI.create(this.serverUrl), this.executor,
			this.restTemplate, null, good, uid, response.getStatusCode(),
			URI.create(this.serverUrl + location.getPath()));
	}

}
