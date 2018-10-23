package pl.edu.pw.ee.pyskp.documentworkflow.data.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Created by piotr on 11.12.16.
 */
@Data
@EqualsAndHashCode(of = "id")
@Document
public class Task {
    @Id
    private ObjectId id;

    @DBRef(lazy = true)
    @Indexed
    private Project project;

    private String name, description;

    private Date creationDate;

    @DBRef
    private User administrator;

    @DBRef
    private List<User> participants;

    @DBRef
    private FileMetadata lastModifiedFile;

    private long numberOfFiles;
}
