/*
 * Copyright 2012 - 2026 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.core.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskManager;

import com.sun.net.httpserver.HttpExchange;

/**
 * the class {@link SystemCommandHandler} offers system related commands to query the current state of tinyMediaManager and to initiate a graceful
 * shutdown - mainly used for automated tasks
 *
 * @author Manuel Laggner
 */
public class SystemCommandHandler extends AbstractCommandHandler {
  private static final Logger LOGGER              = LoggerFactory.getLogger(SystemCommandHandler.class);

  /**
   * the state of tinyMediaManager while it is working
   */
  public static final String  STATE_RUNNING       = "RUNNING";

  /**
   * the state of tinyMediaManager after a shutdown has been requested
   */
  public static final String  STATE_SHUTTING_DOWN = "SHUTTING_DOWN";

  @Override
  public TmmCommandResponse post(HttpExchange httpExchange) throws Exception {
    // convenience for automated tasks: a plain GET on /api/system returns the current status
    if ("GET".equalsIgnoreCase(httpExchange.getRequestMethod())) {
      return createStatusResponse();
    }

    return super.post(httpExchange);
  }

  @Override
  protected TmmCommandResponse processCommands(List<Command> commands) throws Exception {
    if (commands.isEmpty()) {
      return new TmmCommandResponse(400, "no command given - supported actions are 'status' and 'shutdown'");
    }

    TmmCommandResponse response = null;
    for (Command command : commands) {
      if ("status".equals(command.action)) {
        response = createStatusResponse();
      }
      else if ("shutdown".equals(command.action)) {
        response = createShutdownResponse(command);
      }
      else {
        response = new TmmCommandResponse(400, "unknown action '" + command.action + "' - supported actions are 'status' and 'shutdown'");
      }
    }

    return response;
  }

  private TmmCommandResponse createStatusResponse() throws Exception {
    TmmTaskManager taskManager = TmmTaskManager.getInstance();
    boolean mainTaskRunning = taskManager.isMainTaskRunning();
    boolean poolRunning = taskManager.isPoolRunning();

    Map<String, Object> status = new LinkedHashMap<>();
    status.put("version", ReleaseInfo.getVersion());
    status.put("state", TmmHttpServer.getInstance().isShuttingDown() ? STATE_SHUTTING_DOWN : STATE_RUNNING);
    status.put("busy", mainTaskRunning || poolRunning);
    status.put("mainTaskRunning", mainTaskRunning);
    status.put("queuedMainTasks", taskManager.getQueuedMainTaskCount());
    status.put("imageDownloadsRunning", taskManager.isImageDownloadsRunning());
    status.put("poolRunning", poolRunning);

    List<Map<String, Object>> tasks = new ArrayList<>();
    for (TmmTaskHandle task : taskManager.getRunningTasks()) {
      Map<String, Object> taskMap = new LinkedHashMap<>();
      taskMap.put("name", task.getTaskName());
      taskMap.put("type", task.getType().name());
      taskMap.put("state", task.getState().name());
      taskMap.put("workUnits", task.getWorkUnits());
      taskMap.put("progressDone", task.getProgressDone());
      if (StringUtils.isNotBlank(task.getTaskDescription())) {
        taskMap.put("description", task.getTaskDescription());
      }
      tasks.add(taskMap);
    }
    status.put("runningTasks", tasks);

    return new TmmCommandResponse(200, "OK").setPayload(status);
  }

  private TmmCommandResponse createShutdownResponse(Command command) throws Exception {
    String forceArg = command.args == null ? null : command.args.get("force");
    boolean force = Boolean.parseBoolean(forceArg);
    LOGGER.debug("HTTP API: Initiating shutdown - force: {}", force);

    boolean initiated = TmmHttpServer.getInstance().requestShutdown(force);
    if (!initiated) {
      return new TmmCommandResponse(200, "shutdown already initiated");
    }

    String message = force ? "shutdown initiated - tinyMediaManager will exit immediately"
        : "shutdown initiated - no more commands accepted, tinyMediaManager will exit after the last task has finished";
    return new TmmCommandResponse(200, message);
  }
}
