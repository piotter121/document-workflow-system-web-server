import {Component, OnDestroy, OnInit} from '@angular/core';
import {Observable} from 'rxjs';
import {TaskInfo} from '../task-info';
import {ActivatedRoute, ParamMap, Router} from '@angular/router';
import {switchMap} from 'rxjs/operators';
import {TasksService} from '../tasks.service';
import {GlobalsService} from '../../shared/globals.service';
import {UserService} from '../../auth/user.service';
import {UserInfo} from '../../auth/user-info';
import {ToastNotificationService} from '../../shared/toast-notification.service';

@Component({
  selector: 'app-task-details',
  templateUrl: './task-details.component.html',
  styleUrls: ['./task-details.component.css']
})
export class TaskDetailsComponent implements OnInit, OnDestroy {
  task$: Observable<TaskInfo>;
  task: TaskInfo;
  currentUser: UserInfo;

  constructor(private route: ActivatedRoute,
              private tasksService: TasksService,
              private router: Router,
              private globals: GlobalsService,
              private userService: UserService,
              private toastNotification: ToastNotificationService) {
  }

  ngOnInit() {
    this.globals.route = this.route;
    this.task$ = this.route.paramMap.pipe(switchMap((params: ParamMap) => {
      const projectId = params.get('projectId');
      const taskId = params.get('taskId');
      return this.tasksService.getTaskInfo(projectId, taskId);
    }));
    this.task$.subscribe((task: TaskInfo) => this.task = task, () => this.onGetTaskError());
    this.currentUser = this.userService.currentUser;
  }

  ngOnDestroy() {
    this.globals.route = null;
  }

  private onGetTaskError() {
    // noinspection JSIgnoredPromiseFromCall
    this.router.navigate(['..', '..'], {
      relativeTo: this.route
    });
  }

  deleteTask() {
    const projectId = this.task.projectId;
    this.tasksService.deleteTask(this.task.projectId, this.task.id)
      .subscribe(() => {
        this.toastNotification.success('dws.task.details.admin.deleteTask.success');
        // noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['/projects', projectId]);
      }, () => this.toastNotification.error('dws.task.details.admin.deleteTask.failure'));
  }
}
