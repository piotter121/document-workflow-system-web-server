package pl.edu.pw.ee.pyskp.documentworkflow.data.domain;

import com.datastax.driver.core.DataType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("user_project")
public class UserProject {
    @EqualsAndHashCode.Include
    @PrimaryKeyColumn(name = "user_email", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String userEmail;

    @EqualsAndHashCode.Include
    @CassandraType(type = DataType.Name.UUID)
    @PrimaryKeyColumn(name = "project_id", ordinal = 1)
    private UUID projectId;

    @Column("name")
    private String name;

    @Column("creation_date")
    private Date creationDate;

    @Column("last_modified_file")
    private FileSummary lastModifiedFile;

    @Column("number_of_participants")
    private long numberOfParticipants = 1;

    @Column("number_of_tasks")
    private long numberOfTasks = 0;

    @Column("number_of_files")
    private long numberOfFiles = 0;

    public UserProject(User user, Project project) {
        userEmail = user.getEmail();
        projectId = project.getId();
        name = project.getName();
        creationDate = project.getCreationDate();
    }
}
