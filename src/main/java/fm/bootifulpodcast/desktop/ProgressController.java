package fm.bootifulpodcast.desktop;

import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class ProgressController implements Initializable {

	public Label processingLabel;

	public Hyperlink downloadMediaHyperlink;

	private final Messages messages;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		this.processingLabel
				.setText(this.messages.getMessage(getClass(), "processing-status"));
		this.downloadMediaHyperlink
				.setText(this.messages.getMessage(getClass(), "click-to-download"));
	}

}
