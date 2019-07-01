package fm.bootifulpodcast.desktop;

import org.springframework.context.ApplicationEvent;

public class PodcastValidationSuccessEvent extends ApplicationEvent {

	PodcastValidationSuccessEvent(PodcastModel source) {
		super(source);
	}

	@Override
	public PodcastModel getSource() {
		return (PodcastModel) super.getSource();
	}
}
