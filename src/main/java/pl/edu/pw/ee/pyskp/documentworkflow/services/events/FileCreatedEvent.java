package pl.edu.pw.ee.pyskp.documentworkflow.services.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.FileMetadata;

@Getter
public class FileCreatedEvent extends ApplicationEvent {
    private final FileMetadata createdFile;

    public FileCreatedEvent(Object source, FileMetadata createdFile) {
        super(source);
        this.createdFile = createdFile;
    }
}
