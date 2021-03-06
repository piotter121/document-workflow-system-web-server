package pl.edu.pw.ee.pyskp.documentworkflow.services.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.FileMetadata;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.Project;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.Task;
import pl.edu.pw.ee.pyskp.documentworkflow.data.domain.User;
import pl.edu.pw.ee.pyskp.documentworkflow.data.repository.*;
import pl.edu.pw.ee.pyskp.documentworkflow.dtos.project.NewProjectForm;
import pl.edu.pw.ee.pyskp.documentworkflow.dtos.project.ProjectInfoDTO;
import pl.edu.pw.ee.pyskp.documentworkflow.dtos.project.ProjectSummaryDTO;
import pl.edu.pw.ee.pyskp.documentworkflow.exceptions.ProjectNotFoundException;
import pl.edu.pw.ee.pyskp.documentworkflow.exceptions.UserNotFoundException;
import pl.edu.pw.ee.pyskp.documentworkflow.services.ProjectService;
import pl.edu.pw.ee.pyskp.documentworkflow.services.UserService;
import pl.edu.pw.ee.pyskp.documentworkflow.services.events.*;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

/**
 * Created by piotr on 29.12.16.
 */
@RequiredArgsConstructor
@Service
public class ProjectServiceImpl implements ProjectService {
    @NonNull
    private final UserService userService;

    @NonNull
    private final ProjectRepository projectRepository;

    @NonNull
    private final TaskRepository taskRepository;

    @NonNull
    private final FileMetadataRepository fileMetadataRepository;

    @NonNull
    private final VersionRepository versionRepository;

    @NonNull
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSummaryDTO> getUserParticipatedProjects(String userEmail) throws UserNotFoundException {
        User user = userRepository.findOneByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));
        Stream<Project> administratedProjects = projectRepository.findByAdministrator(user);

        Stream<Task> participatedTasks = taskRepository.findByParticipantsContainingOrAdministrator(user, user);
        Stream<Project> participatedProjects = participatedTasks.map(Task::getProject).distinct();

        return concat(administratedProjects, participatedProjects)
                .distinct()
                .map(ProjectSummaryDTO::fromProject)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ObjectId createNewProjectFromForm(NewProjectForm formDTO) {
        User currentUser = userService.getCurrentUser();
        Project project = new Project();
        project.setName(formDTO.getName().trim());
        project.setDescription(formDTO.getDescription().trim());
        project.setAdministrator(currentUser);
        project.setCreationDate(new Date());
        project.setNumberOfFiles(0);
        project.setNumberOfParticipants(1);
        project.setNumberOfTasks(0);
        project = projectRepository.save(project);
        return project.getId();
    }

    private Project getProject(ObjectId projectId) throws ProjectNotFoundException {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId.toString()));
    }

    @Override
    @Transactional(rollbackFor = ProjectNotFoundException.class)
    public void deleteProject(ObjectId projectId) throws ProjectNotFoundException {
        Project project = getProject(projectId);

        List<Task> tasks = taskRepository.findByProject(project).collect(Collectors.toList());

        List<FileMetadata> files = fileMetadataRepository.findByTaskIn(tasks);

        versionRepository.deleteByFileIn(files);
        fileMetadataRepository.deleteAll(files);
        taskRepository.deleteAll(tasks);
        projectRepository.delete(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectInfoDTO getProjectInfo(ObjectId projectId) throws ProjectNotFoundException {
        Project project = this.getProject(projectId);
        List<Task> projectTasks = taskRepository.findByProject(project)
                .sorted(comparing((Task task) -> {
                    FileMetadata lastModifiedFile = task.getLastModifiedFile();
                    if (lastModifiedFile != null) {
                        return lastModifiedFile.getLatestVersion().getSaveDate();
                    } else {
                        return task.getCreationDate();
                    }
                }).reversed())
                .collect(Collectors.toList());
        return ProjectInfoDTO.fromProjectAndTasks(project, projectTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public String getProjectName(ObjectId projectId) throws ProjectNotFoundException {
        return getProject(projectId).getName();
    }

    @Override
    @Transactional
    @EventListener
    @Order(2)
    public void processFileCreatedEvent(FileCreatedEvent event) {
        FileMetadata createdFile = event.getCreatedFile();
        Project project = createdFile.getTask().getProject();
        project.setLastModifiedFile(createdFile);
        project.setNumberOfFiles(project.getNumberOfFiles() + 1);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    @EventListener
    @Order(2)
    public void processFileDeletedEvent(FileDeletedEvent event) {
        Project project = event.getDeletedFile().getTask().getProject();
        List<Task> projectTasks = taskRepository.findByProject(project).collect(Collectors.toList());
        long numberOfProjectFiles = fileMetadataRepository.countByTaskIn(projectTasks);
        project.setNumberOfFiles(numberOfProjectFiles);
        FileMetadata lastModifiedFile = projectTasks.stream()
                .map(Task::getLastModifiedFile)
                .filter(Objects::nonNull)
                .max(comparing(file -> file.getLatestVersion().getSaveDate()))
                .orElse(null);
        project.setLastModifiedFile(lastModifiedFile);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    @EventListener
    public void processTaskCreatedEvent(TaskCreatedEvent event) {
        Task createdTask = event.getCreatedTask();
        Project project = createdTask.getProject();
        project.setNumberOfTasks(project.getNumberOfTasks() + 1);
        Stream<Task> projectTasks = taskRepository.findByProject(project);
        long numberOfProjectParticipants = getNumberOfProjectParticipants(projectTasks, project);
        project.setNumberOfParticipants(numberOfProjectParticipants);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    @EventListener
    public void processTaskDeletedEvent(TaskDeletedEvent event) {
        Task deletedTask = event.getDeletedTask();
        Project project = deletedTask.getProject();
        project.setNumberOfTasks(project.getNumberOfTasks() - 1);
        List<Task> projectTasks = taskRepository.findByProject(project).collect(Collectors.toList());
        long numberOfProjectParticipants = getNumberOfProjectParticipants(projectTasks.stream(), project);
        project.setNumberOfParticipants(numberOfProjectParticipants);
        project.setNumberOfFiles(project.getNumberOfFiles() - deletedTask.getNumberOfFiles());
        FileMetadata lastModifiedFile = projectTasks.stream()
                .map(Task::getLastModifiedFile)
                .max(comparing((FileMetadata file) -> file.getLatestVersion().getSaveDate()).reversed())
                .orElse(null);
        project.setLastModifiedFile(lastModifiedFile);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    @EventListener
    public void processParticipantAddedToTaskEvent(ParticipantAddedToTaskEvent event) {
        Task modifiedTask = event.getModifiedTask();
        updateProjectNumberOfParticipants(modifiedTask);
    }

    @Override
    @Transactional
    @EventListener
    public void processParticipantRemovedFromTaskEvent(ParticipantRemovedFromTaskEvent event) {
        Task modifiedTask = event.getModifiedTask();
        updateProjectNumberOfParticipants(modifiedTask);
    }

    @Override
    @Transactional
    @EventListener
    @Order(3)
    public void processVersionCreatedEvent(VersionCreatedEvent event) {
        FileMetadata modifiedFile = event.getModifiedFile();
        Task task = modifiedFile.getTask();
        Project project = task.getProject();
        project.setLastModifiedFile(modifiedFile);
        projectRepository.save(project);
    }

    private void updateProjectNumberOfParticipants(Task modifiedTask) {
        Project project = modifiedTask.getProject();
        Stream<Task> projectTasks = taskRepository.findByProject(project);
        long numberOfProjectParticipants = getNumberOfProjectParticipants(projectTasks, project);
        project.setNumberOfParticipants(numberOfProjectParticipants);
        projectRepository.save(project);
    }

    private long getNumberOfProjectParticipants(Stream<Task> projectTasks, Project project) {
        Stream<User> projectParticipants = projectTasks
                .flatMap(task -> concat(task.getParticipants().stream(), of(task.getAdministrator())));
        return concat(projectParticipants, of(project.getAdministrator())).distinct().count();
    }
}
