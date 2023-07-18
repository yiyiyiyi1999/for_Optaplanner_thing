package org.optaplanner.examples.taskassigning.app;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.examples.common.persistence.AbstractSolutionImporter;
import org.optaplanner.examples.common.persistence.generator.StringDataGenerator;
import org.optaplanner.examples.nqueens.domain.NQueens;
import org.optaplanner.examples.nqueens.persistence.NQueensGenerator;
import org.optaplanner.examples.taskassigning.domain.*;
import org.optaplanner.examples.taskassigning.persistence.TaskAssigningGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

public class TaskAssigningHelloWorld {
    public static final int BASE_DURATION_MINIMUM = 30;
    public static final int BASE_DURATION_MAXIMUM = 90;
    public static final int BASE_DURATION_AVERAGE = BASE_DURATION_MINIMUM + BASE_DURATION_MAXIMUM / 2;
    private static final int SKILL_SET_SIZE_MINIMUM = 2;
    private static final int SKILL_SET_SIZE_MAXIMUM = 4;

    protected static Random random;

//    protected final static Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        // Build the Solver
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.createFromXmlResource(
                "org/optaplanner/examples/taskAssigning/taskAssigningSolverConfig.xml");
        Solver<TaskAssigningSolution> solver = solverFactory.buildSolver();

        TaskAssigningSolution unsolved = writeTaskAssigningSolution(50, 5);

        // Solve the problem
        TaskAssigningSolution solved = solver.solve(unsolved);

        System.out.println("solved");
    }

    private final static StringDataGenerator skillNameGenerator = new StringDataGenerator()
            .addPart(true, 0,
                    "Problem",
                    "Team",
                    "Business",
                    "Risk",
                    "Creative",
                    "Strategic",
                    "Customer",
                    "Conflict",
                    "IT",
                    "Academic")
            .addPart(true, 1,
                    "Solving",
                    "Building",
                    "Storytelling",
                    "Management",
                    "Thinking",
                    "Planning",
                    "Service",
                    "Resolution",
                    "Engineering",
                    "Research");
    private final static StringDataGenerator taskTypeNameGenerator = new StringDataGenerator()
            .addPart(true, 0,
                    "Improve",
                    "Expand",
                    "Shrink",
                    "Approve",
                    "Localize",
                    "Review",
                    "Clean",
                    "Merge",
                    "Double",
                    "Optimize")
            .addPart(true, 1,
                    "Sales",
                    "Tax",
                    "VAT",
                    "Legal",
                    "Cloud",
                    "Marketing",
                    "IT",
                    "Contract",
                    "Financial",
                    "Advertisement")
            .addPart(false, 2,
                    "Software",
                    "Development",
                    "Accounting",
                    "Management",
                    "Facilities",
                    "Writing",
                    "Productization",
                    "Lobbying",
                    "Engineering",
                    "Research");
    private final static StringDataGenerator customerNameGenerator = StringDataGenerator.buildCompanyNames();
    private final static StringDataGenerator employeeNameGenerator = StringDataGenerator.buildFullNames();

    private static TaskAssigningSolution writeTaskAssigningSolution(int taskListSize, int employeeListSize) {
        int skillListSize = SKILL_SET_SIZE_MAXIMUM + (int) Math.log(employeeListSize);
        int taskTypeListSize = taskListSize / 5;
        int customerListSize = Math.min(taskTypeListSize, employeeListSize * 3);
        TaskAssigningSolution solution = createTaskAssigningSolution(taskListSize, skillListSize, employeeListSize, taskTypeListSize, customerListSize);
        return solution;
    }

    public static TaskAssigningSolution createTaskAssigningSolution(int taskListSize, int skillListSize,
                                                             int employeeListSize, int taskTypeListSize, int customerListSize) {
        random = new Random(37);
        TaskAssigningSolution solution = new TaskAssigningSolution(0L);

        createSkillList(solution, skillListSize);
        createCustomerList(solution, customerListSize);
        createEmployeeList(solution, employeeListSize);
        createTaskTypeList(solution, taskTypeListSize);
        createTaskList(solution, taskListSize);
        solution.setFrozenCutoff(0);

        BigInteger a = AbstractSolutionImporter.factorial(taskListSize + employeeListSize - 1);
        BigInteger b = AbstractSolutionImporter.factorial(employeeListSize - 1);
        BigInteger possibleSolutionSize = (a == null || b == null) ? null : a.divide(b);
        return solution;
    }

    private static void createSkillList(TaskAssigningSolution solution, int skillListSize) {
        List<Skill> skillList = new ArrayList<>(skillListSize);
        skillNameGenerator.predictMaximumSizeAndReset(skillListSize);
        for (int i = 0; i < skillListSize; i++) {
            String skillName = skillNameGenerator.generateNextValue();
            Skill skill = new Skill(i, skillName);
//            logger.trace("Created skill with skillName ({}).", skillName);
            skillList.add(skill);
        }
        solution.setSkillList(skillList);
    }

    private static void createCustomerList(TaskAssigningSolution solution, int customerListSize) {
        List<Customer> customerList = new ArrayList<>(customerListSize);
        customerNameGenerator.predictMaximumSizeAndReset(customerListSize);
        for (int i = 0; i < customerListSize; i++) {
            String customerName = customerNameGenerator.generateNextValue();
            Customer customer = new Customer(i, customerName);
//            logger.trace("Created skill with customerName ({}).", customerName);
            customerList.add(customer);
        }
        solution.setCustomerList(customerList);
    }

    private static void createEmployeeList(TaskAssigningSolution solution, int employeeListSize) {
        List<Skill> skillList = solution.getSkillList();
        List<Customer> customerList = solution.getCustomerList();
        Affinity[] affinities = Affinity.values();
        List<Employee> employeeList = new ArrayList<>(employeeListSize);
        int skillListIndex = 0;
        employeeNameGenerator.predictMaximumSizeAndReset(employeeListSize);
        for (int i = 0; i < employeeListSize; i++) {
            String fullName = employeeNameGenerator.generateNextValue();
            Employee employee = new Employee(i, fullName);
            int skillSetSize = SKILL_SET_SIZE_MINIMUM + random.nextInt(SKILL_SET_SIZE_MAXIMUM - SKILL_SET_SIZE_MINIMUM);
            if (skillSetSize > skillList.size()) {
                skillSetSize = skillList.size();
            }
            Set<Skill> skillSet = new LinkedHashSet<>(skillSetSize);
            for (int j = 0; j < skillSetSize; j++) {
                skillSet.add(skillList.get(skillListIndex));
                skillListIndex = (skillListIndex + 1) % skillList.size();
            }
            employee.setSkillSet(skillSet);
            Map<Customer, Affinity> affinityMap = new LinkedHashMap<>(customerList.size());
            for (Customer customer : customerList) {
                affinityMap.put(customer, affinities[random.nextInt(affinities.length)]);
            }
            employee.setAffinityMap(affinityMap);
            employee.setTasks(new ArrayList<>());
//            logger.trace("Created employee with fullName ({}).", fullName);
            employeeList.add(employee);
        }
        solution.setEmployeeList(employeeList);
    }

    private static void createTaskTypeList(TaskAssigningSolution solution, int taskTypeListSize) {
        List<Employee> employeeList = solution.getEmployeeList();
        List<TaskType> taskTypeList = new ArrayList<>(taskTypeListSize);
        Set<String> codeSet = new LinkedHashSet<>(taskTypeListSize);
        taskTypeNameGenerator.predictMaximumSizeAndReset(taskTypeListSize);
        for (int i = 0; i < taskTypeListSize; i++) {
            String title = taskTypeNameGenerator.generateNextValue();
            String code;
            switch (title.replaceAll("[^ ]", "").length() + 1) {
                case 3:
                    code = title.replaceAll("(\\w)\\w* (\\w)\\w* (\\w)\\w*", "$1$2$3");
                    break;
                case 2:
                    code = title.replaceAll("(\\w)\\w* (\\w)\\w*", "$1$2");
                    break;
                case 1:
                    code = title.replaceAll("(\\w)\\w*", "$1");
                    break;
                default:
                    throw new IllegalStateException("Cannot convert title (" + title + ") into a code.");
            }
            if (codeSet.contains(code)) {
                int codeSuffixNumber = 1;
                while (codeSet.contains(code + codeSuffixNumber)) {
                    codeSuffixNumber++;
                }
                code = code + codeSuffixNumber;
            }
            codeSet.add(code);
            TaskType taskType = new TaskType(i, title, code,
                    BASE_DURATION_MINIMUM + random.nextInt(BASE_DURATION_MAXIMUM - BASE_DURATION_MINIMUM));
            Employee randomEmployee = employeeList.get(random.nextInt(employeeList.size()));
            ArrayList<Skill> randomSkillList = new ArrayList<>(randomEmployee.getSkillSet());
            Collections.shuffle(randomSkillList, random);
            int requiredSkillListSize = 1 + random.nextInt(randomSkillList.size() - 1);
            taskType.setRequiredSkillList(new ArrayList<>(randomSkillList.subList(0, requiredSkillListSize)));
//            logger.trace("Created taskType with title ({}).", title);
            taskTypeList.add(taskType);
        }
        solution.setTaskTypeList(taskTypeList);
    }

    private static void createTaskList(TaskAssigningSolution solution, int taskListSize) {
        List<TaskType> taskTypeList = solution.getTaskTypeList();
        List<Customer> customerList = solution.getCustomerList();
        Priority[] priorities = Priority.values();
        List<Task> taskList = new ArrayList<>(taskListSize);
        Map<TaskType, Integer> maxIndexInTaskTypeMap = new LinkedHashMap<>(taskTypeList.size());
        for (int i = 0; i < taskListSize; i++) {
            TaskType taskType = taskTypeList.get(random.nextInt(taskTypeList.size()));
            Integer indexInTaskType = maxIndexInTaskTypeMap.get(taskType);
            if (indexInTaskType == null) {
                indexInTaskType = 1;
            } else {
                indexInTaskType++;
            }
            maxIndexInTaskTypeMap.put(taskType, indexInTaskType);
            Task task = new Task(i, taskType, indexInTaskType, customerList.get(random.nextInt(customerList.size())), 0,
                    priorities[random.nextInt(priorities.length)]);
            taskList.add(task);
        }
        solution.setTaskList(taskList);
    }
}
