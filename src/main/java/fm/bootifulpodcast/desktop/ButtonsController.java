package fm.bootifulpodcast.desktop;

import fm.bootifulpodcast.desktop.client.ApiConnectedEvent;
import fm.bootifulpodcast.desktop.client.ApiDisconnectedEvent;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Component
public class ButtonsController implements Initializable {

	public Label connectedIcon;

	private final ImageView connectedImageView, disconnectedImageView;
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private final AtomicBoolean validPodcast = new AtomicBoolean(false);
	public Button newPodcastButton, publishButton, saveMediaToFileButton;
	public HBox buttons;

	ButtonsController() {
		this.disconnectedImageView = this.buildImageViewFrom(new ClassPathResource("images/disconnected-icon.png"));
		this.connectedImageView = this.buildImageViewFrom(new ClassPathResource("images/connected-icon.png"));
	}

	private void updateConnectedIcon(ImageView iv) {
		Platform.runLater(() -> this.connectedIcon.setGraphic(iv));
	}

	@EventListener(ApiDisconnectedEvent.class)
	public void disconnect() {
		log.info("disconnected");
		this.connected.set(false);
		this.updateConnectedIcon(this.disconnectedImageView);
		this.evaluatePublishButtonState();
	}

	@EventListener(ApiConnectedEvent.class)
	public void connected() {
		log.info("connected");
		this.connected.set(true);
		this.updateConnectedIcon(this.connectedImageView);
		this.evaluatePublishButtonState();
	}

	@EventListener
	public void invalidPodcast(PodcastValidationFailedEvent failed) {
		log.debug("the podcast is invalid.");
		this.validPodcast.set(false);
		this.evaluatePublishButtonState();
	}

	@EventListener
	public void validPodcast(PodcastValidationSuccessEvent success) {
		log.debug("the podcast is valid.");
		this.validPodcast.set(true);
		this.evaluatePublishButtonState();
	}

	private void evaluatePublishButtonState() {
		var active = (this.connected.get() && this.validPodcast.get());
		publishButton.setDisable(!active);
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		var children = this.buttons.getChildren();
		children.forEach(n -> n.setDisable(true));
		children.clear();
		children.addAll(this.newPodcastButton, this.publishButton);
		this.newPodcastButton.setDisable(false);
		this.connectedIcon.setGraphic(this.disconnectedImageView);
	}

	@SneakyThrows
	private ImageView buildImageViewFrom(Resource resource) {
		try (var in = resource.getInputStream()) {
			var imageView = new ImageView(new Image(in));
			imageView.setSmooth(true);
			imageView.setPreserveRatio(true);
			imageView.setFitHeight(30);
			return imageView;
		}
	}
}
