package fm.bootifulpodcast.desktop;

import fm.bootifulpodcast.desktop.client.ApiClient;
import fm.bootifulpodcast.desktop.client.ApiConnectedEvent;
import fm.bootifulpodcast.desktop.client.ApiDisconnectedEvent;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Component
public class ButtonsController implements Initializable {

	public Label connectedIcon;

	private final ImageView connectedImageView, disconnectedImageView;

	private final AtomicBoolean connected = new AtomicBoolean(false);

	private final AtomicReference<PodcastModel> podcast = new AtomicReference<>();

	private final ApiClient client;

	public Button newPodcastButton, publishButton, saveMediaToFileButton;

	public HBox buttons;

	ButtonsController(ApiClient client) {
		this.client = client;
		this.disconnectedImageView = FxUtils.buildImageViewFromResource(
				new ClassPathResource("images/disconnected-icon.png"));
		this.connectedImageView = FxUtils.buildImageViewFromResource(
				new ClassPathResource("images/connected-icon.png"));
		List.of(this.disconnectedImageView, this.connectedImageView)
				.forEach(img -> img.setFitHeight(30));
	}

	private void updateConnectedIcon(ImageView iv) {
		Platform.runLater(() -> this.connectedIcon.setGraphic(iv));
	}

	@EventListener
	public void disconnect(ApiDisconnectedEvent e) {
		log.info("disconnected (" + e.getClass().getName() + ")");
		this.connected.set(false);
		this.updateConnectedIcon(this.disconnectedImageView);
		this.evaluatePublishButtonState();
	}

	@EventListener
	public void connected(ApiConnectedEvent e) {
		log.info("connected (" + e.getClass().getName() + ")");
		this.connected.set(true);
		this.updateConnectedIcon(this.connectedImageView);
		this.evaluatePublishButtonState();
	}

	@EventListener
	public void invalidPodcast(PodcastValidationFailedEvent pvfe) {
		log.debug("the podcast is invalid.");
		this.podcast.set(null);
		this.evaluatePublishButtonState();
	}

	@EventListener
	public void productionStarted(PodcastProductionStartedEvent pse) {
		List.of(this.publishButton, this.newPodcastButton)
				.forEach(btn -> btn.setDisable(true));
	}

	@EventListener
	public void productionFinished(PodcastProductionCompletedEvent ppce) {
		List.of(this.newPodcastButton).forEach(btn -> btn.setDisable(false));
	}

	@EventListener
	public void validPodcast(PodcastValidationSuccessEvent pvse) {
		log.debug("the podcast is valid.");
		this.podcast.set(pvse.getSource());
		this.evaluatePublishButtonState();
	}

	private void evaluatePublishButtonState() {
		var canPublish = (this.connected.get() && this.podcast.get() != null);
		publishButton.setDisable(!canPublish);
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		var children = this.buttons.getChildren();
		children.forEach(n -> n.setDisable(true));
		children.clear();
		children.addAll(this.newPodcastButton, this.publishButton);
		this.newPodcastButton.setDisable(false);
		this.connectedIcon.setGraphic(this.disconnectedImageView);
		this.publishButton.setOnMouseClicked(e -> {
			var model = this.podcast.get();
			var uuid = UUID.randomUUID().toString();
			this.client.produce(uuid, model.titleProperty().get(),
					model.descriptionProperty().get(),
					model.introductionFileProperty().get(),
					model.interviewFileProperty().get());
		});
	}

}
