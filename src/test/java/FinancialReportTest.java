import static org.junit.Assert.assertEquals;

import java.util.List;

import junit.framework.AssertionFailedError;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FinancialReportTest
{
   ProcessEngine processEngine;
   RepositoryService repositoryService;
   RuntimeService runtimeService;
   Deployment deployment;
   TaskService taskService;

   @Before
   public void deploy()
   {
      // Create Activiti process engine.
      // This looks for activiti.cfg.xml in the classpath.
      processEngine = ProcessEngines.getDefaultProcessEngine();

      // Get Activiti services
      repositoryService = processEngine.getRepositoryService();
      runtimeService = processEngine.getRuntimeService();

      // Deploy the process definition
      deployment = repositoryService.createDeployment()
            .addClasspathResource("financial-report.bpmn20.xml").deploy();

      taskService = processEngine.getTaskService();
   }

   @Test
   public void testInstance()
   {
      // Start a process instance
      ProcessInstance processInstance = runtimeService
            .startProcessInstanceByKey("financialReport");

      // Verify that fozzie is one of the users that can write the report
      List<Task> tasks = taskService.createTaskQuery()
            .taskCandidateUser("fozzie").list();
      assertEquals(1, tasks.size());
      Task task = tasks.get(0);
      assertEquals("Write monthly financial report", task.getName());

      // Have fozzie claim and complete the report writing task
      taskService.claim(task.getId(), "fozzie");
      tasks = taskService.createTaskQuery().taskAssignee("fozzie").list();
      System.out.println(tasks);
      assertEquals(1, tasks.size());
      taskService.complete(task.getId());
      tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
      System.out.println(tasks);
      assertEquals(0, tasks.size());

      // Verify that kermit is one of the users that can verify the report
      tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
      assertEquals(1, tasks.size());
      assertEquals("Verify monthly financial report", tasks.get(0).getName());

      // Complete the task and verify that the process has ended
      taskService.complete(tasks.get(0).getId());
      assertProcessEnded(processInstance.getId());
   }

   @After
   public void undeploy()
   {
      repositoryService.deleteDeployment(deployment.getId(), true);
   }

   public void assertProcessEnded(final String processInstanceId)
   {
      ProcessInstance processInstance = processEngine.getRuntimeService()
            .createProcessInstanceQuery().processInstanceId(processInstanceId)
            .singleResult();
      if (processInstance != null)
      {
         throw new AssertionFailedError("Expected finished process instance '"
               + processInstanceId + "' but it was still in the db");
      }
   }
}