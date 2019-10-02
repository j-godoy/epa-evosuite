package org.evosuite.coverage.epa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.evosuite.Properties;
import org.evosuite.Properties.Criterion;
import org.evosuite.TestGenerationContext;
import org.evosuite.epa.EpaAction;
import org.evosuite.epa.EpaActionPrecondition;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.ArrayUtil;
import org.evosuite.utils.FileIOUtils;

/**
 * EPA-related utilities
 */
public class EPAUtils {

	public static boolean epaStateMethodExists(EPAState epaState, Class<?> objectClass) {
		try {
			getEpaStateMethod(epaState, objectClass);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	public static Method getEpaStateMethod(EPAState epaState, Class<?> objectClass) throws NoSuchMethodException {
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			final Optional<Method> methodOptional = Arrays.stream(currentClass.getDeclaredMethods())
					.filter(declaredMethod -> methodIsAnnotatedAs(declaredMethod, "EpaState", epaState.getName()))
					.findAny();
			if (methodOptional.isPresent())
				return methodOptional.get();

			currentClass = currentClass.getSuperclass();
		}
		throw new NoSuchMethodException("Boolean query method for state " + epaState + " was not found in class "
				+ objectClass.getName() + " or any superclass");
	}

	/**
	 * Returns true if the method has the @EpaAction annotation.
	 * 
	 * @param method
	 * @return
	 */
	private static Annotation getEpaActionAnnotation(Executable method) {
		return getAnnotation(method, EpaAction.class);
	}

	/**
	 * Returns true if the method has the @EpaActionPrecondition annotation.
	 * 
	 * @param method
	 * @return
	 */
	private static Annotation getEpaActionPreconditionAnnotation(Method method) {
		return getAnnotation(method, EpaActionPrecondition.class);
	}

	private static Annotation getAnnotation(Executable executable, Class<?> annotationClass) {
		for (Annotation annotation : executable.getDeclaredAnnotations()) {
			if (annotation.annotationType().getName().equals(annotationClass.getName())) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns the value of the field "name" of the annotation @EpaAction
	 * 
	 * @param annotation
	 *            an @EpaAction annotation
	 * 
	 * @return
	 * @throws EvosuiteError
	 */
	private static String getEpaActionAnnotationName(Annotation annotation) throws EvosuiteError {
		try {
			final Method nameMethod = annotation.getClass().getDeclaredMethod("name");
			Object idResult = nameMethod.invoke(annotation);
			if (idResult.getClass() == String.class) {
				final String actionId = (String) idResult;
				return actionId;
			} else {
				return null;
			}
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new EvosuiteError(e);
		}
	}

	/**
	 * Collects a maps from actionId to the methods that are labelled with
	 * a @EpaAction annotation
	 * 
	 * @param objectClass
	 * @return
	 */
	public static Map<String, Set<Method>> getEpaActionMethods(Class<?> objectClass) {
		Map<String, Set<Method>> epaActionMethodsMap = new HashMap<String, Set<Method>>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			for (Method method : currentClass.getDeclaredMethods()) {
				Annotation epaActionAnnotation = getEpaActionAnnotation(method);
				if (epaActionAnnotation != null) {
					String actionId = getEpaActionAnnotationName(epaActionAnnotation);
					if (actionId == null) {
						throw new EvosuiteError("name field was not found in an @EpaAction annotation");
					}
					if (!epaActionMethodsMap.containsKey(actionId)) {
						epaActionMethodsMap.put(actionId, new HashSet<Method>());
					}
					epaActionMethodsMap.get(actionId).add(method);
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return epaActionMethodsMap;
	}

	public static Map<String, Set<Constructor<?>>> getEpaActionConstructors(Class<?> objectClass) {
		Map<String, Set<Constructor<?>>> epaActionMethodsMap = new HashMap<String, Set<Constructor<?>>>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			for (Constructor<?> constructor : currentClass.getDeclaredConstructors()) {
				Annotation epaActionAnnotation = getEpaActionAnnotation(constructor);
				if (epaActionAnnotation != null) {
					String actionId = getEpaActionAnnotationName(epaActionAnnotation);
					if (actionId == null) {
						throw new EvosuiteError("name field was not found in an @EpaAction annotation");
					}
					if (!epaActionMethodsMap.containsKey(actionId)) {
						epaActionMethodsMap.put(actionId, new HashSet<Constructor<?>>());
					}
					epaActionMethodsMap.get(actionId).add(constructor);
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return epaActionMethodsMap;
	}

	/**
	 * Collects a maps from actionId to each @EpaActionPrecondition method
	 * 
	 * @param objectClass
	 * @return
	 */
	public static Map<String, Method> getEpaActionPreconditionMethods(Class<?> objectClass) {
		Map<String, Method> epaActionPreconditionMethodsMap = new HashMap<String, Method>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			for (Method method : currentClass.getDeclaredMethods()) {
				Annotation epaActionPreconditionAnnotation = getEpaActionPreconditionAnnotation(method);
				if (epaActionPreconditionAnnotation != null) {
					String actionId = getEpaActionAnnotationName(epaActionPreconditionAnnotation);
					if (actionId == null) {
						throw new EvosuiteError("name field was not found in an @EpaActionPrecondition annotation");
					}
					if (epaActionPreconditionMethodsMap.containsKey(actionId)) {
						throw new EvosuiteError("Found repeated @EpaActionPrecondition methods for action " + actionId);
					}
					epaActionPreconditionMethodsMap.put(actionId, method);
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return epaActionPreconditionMethodsMap;
	}

	public static Set<Method> getEpaActionMethods(String actionName, Class<?> objectClass) {
		Set<Method> methods = new HashSet<Method>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			final Set<Method> methodForClass = Arrays.stream(currentClass.getDeclaredMethods())
					.filter(declaredMethod -> methodIsAnnotatedAs(declaredMethod, "EpaAction", actionName))
					.collect(Collectors.toSet());
			methods.addAll(methodForClass);
			currentClass = currentClass.getSuperclass();
		}
		return methods;
	}

	private static boolean constructorIsAnnotatedAs(Constructor<?> constructor, String annotationName,
			String stateName) {
		Annotation[] declaredAnnotations = constructor.getDeclaredAnnotations();
		return Arrays.stream(declaredAnnotations)
				// Is annotation name the same?
				.filter(annotation -> {
					return annotation.annotationType().getSimpleName().equals(annotationName);
				})
				// Is ID the same?
				.filter(annotation -> {
					try {
						final Method idMethod = annotation.getClass().getDeclaredMethod("name");
						Object idResult = idMethod.invoke(annotation);
						if (idResult.getClass() == String.class) {
							final String idResultAsString = (String) idResult;
							if (idResultAsString.equals(stateName))
								return true;
						}
					} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
					}
					return false;
				}).findAny().isPresent();
	}

	private static boolean methodIsAnnotatedAs(Method method, String annotationName, String stateName) {
		Annotation[] declaredAnnotations = method.getDeclaredAnnotations();
		return Arrays.stream(declaredAnnotations)
				// Is annotation name the same?
				.filter(annotation -> {
					return annotation.annotationType().getSimpleName().equals(annotationName);
				})
				// Is ID the same?
				.filter(annotation -> {
					try {
						final Method idMethod = annotation.getClass().getDeclaredMethod("name");
						Object idResult = idMethod.invoke(annotation);
						if (idResult.getClass() == String.class) {
							final String idResultAsString = (String) idResult;
							if (idResultAsString.equals(stateName))
								return true;
						}
					} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
					}
					return false;
				}).findAny().isPresent();
	}

	public static Set<Constructor<?>> getEpaActionConstructors(String actionName, Class<? extends Object> objectClass) {
		Set<Constructor<?>> constructors = new HashSet<Constructor<?>>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			final Set<Constructor<?>> methodForClass = Arrays.stream(currentClass.getDeclaredConstructors())
					.filter(declaredMethod -> constructorIsAnnotatedAs(declaredMethod, "EpaAction", actionName))
					.collect(Collectors.toSet());
			constructors.addAll(methodForClass);
			currentClass = currentClass.getSuperclass();
		}
		return constructors;
	}
	
	public static int checkActionAndPreconditionsAnnotationsForMiningAndgetActionsSize() throws EvosuiteError {
		Class<?> targetClass;
		try {
			targetClass = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(Properties.TARGET_CLASS);
		} catch (ClassNotFoundException e) {
			throw new EvosuiteError(e);
		}

		Map<String, Set<Constructor<?>>> actionConstructorMap = EPAUtils.getEpaActionConstructors(targetClass);
		Map<String, Set<Method>> actionMethodsMap = EPAUtils.getEpaActionMethods(targetClass);
		Map<String, Method> preconditionMethodsMap = EPAUtils.getEpaActionPreconditionMethods(targetClass);

		EPAUtils.checkActionAndPreconditionsAnnotationsForEpaMining(actionMethodsMap, preconditionMethodsMap);

		Set<String> actionIds = new HashSet<String>(actionMethodsMap.keySet());
		actionIds.addAll(actionConstructorMap.keySet());
		
		return actionIds.size();
	}
	
	private static void checkActionAndPreconditionsAnnotationsForEpaMining(Map<String, Set<Method>> actionMethodsMap,
			Map<String, Method> preconditionMethodsMap) throws EvosuiteError {
		for (String actionId : actionMethodsMap.keySet()) {
			if (!preconditionMethodsMap.containsKey(actionId)) {
				throw new EvosuiteError("@EpaActionPrecondition annotation missing for action " + actionId);
			}
		}

		for (String actionId : preconditionMethodsMap.keySet()) {
			if (!actionMethodsMap.containsKey(actionId)) {
				throw new EvosuiteError("Missing @EpaAction annotation for action " + actionId);
			}
		}
	}
	
	public static boolean currCriteriaRequireEPAXML()
	{
		return ArrayUtil.contains(Properties.CRITERION, Properties.Criterion.EPATRANSITION)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.EPAERROR)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.EPAEXCEPTION)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.EPAADJACENTEDGES);
	}
	
	public static void saveInferredEPA(TestSuiteChromosome testSuite, String pathToSaveEPA)
	{
		//Debe generar el xml de la epa antes de llamar a analyzeCoverage porque pierde la instrumentación para
		// tener los callbacks a EPAMonitor
		if (pathToSaveEPA != null) {
			Set<EPATrace> traces = new HashSet<EPATrace>();
			for (TestChromosome test : testSuite.getTestChromosomes()) {
				// delete all statements leading to security exceptions
				ExecutionResult result = test.getLastExecutionResult();
				if (result == null) {
					result = TestCaseExecutor.runTest(test.getTestCase());
				}
				Set<EPATrace> resultTraces = result.getTrace().getEPATraces();
				traces.addAll(resultTraces);
			}
			saveInferredEPA(traces, pathToSaveEPA);
		}
	}
	
	public static void saveInferredEPA(List<ExecutionResult> results, String pathToSaveEPA)
	{
		if (pathToSaveEPA != null /*&& !EPAUtils.currCriteriaRequireEPAXML()*/) {
			Set<EPATrace> traces = new HashSet<EPATrace>();
			for (ExecutionResult result: results) {
				Set<EPATrace> resultTraces = result.getTrace().getEPATraces();
				traces.addAll(resultTraces);
			}
			saveInferredEPA(traces, pathToSaveEPA);
		}
	}
	
	private static void saveInferredEPA(Set<EPATrace> traces, String pathToSaveEPA)
	{
		try {
			EPA inferredAutomata = EPAFactory.buildEPA(traces);
			EPAXMLPrinter xmlPrinter = new EPAXMLPrinter();
			String xmlFilename = pathToSaveEPA;
			String epa_xml_str = xmlPrinter.toXML(inferredAutomata);
			FileIOUtils.writeFile(epa_xml_str, xmlFilename);
			EPADotPrinter printer = new EPADotPrinter();
			String dot_str = printer.toDot(inferredAutomata);
			FileIOUtils.writeFile(dot_str, xmlFilename.replace(".xml", ".dot"));
		} catch (MalformedEPATraceException e) {
			throw new EvosuiteError(e);
		}
		
	}

	public static boolean isActionEnabledInInferredState(String actionName, EPAState currentEpaState) {
		List<String> actions = getActionNamesFromStateName(currentEpaState.getName());
		if (actions.size() == 0)
			throw new EvosuiteError("EpaSate " + currentEpaState + " does not contains actions names. Only inferred states contains actions in epa states");
		if(actions.size() == 1) // state with constructor only
			return true;
		for (String action : actions) {
			String extractedAction = action.split("=")[0];
			if(!extractedAction.equals(actionName))
				continue;
			return action.contains("true");
		}
		return false;
	}
	
	private static List<String> getActionNamesFromStateName(String epaStateName) {
		if(epaStateName.startsWith("["))
			epaStateName = epaStateName.substring(1);
		if(epaStateName.endsWith("]"))
			epaStateName = epaStateName.substring(0, epaStateName.length()-1);

		List<String> actionNames = new ArrayList<>();
		boolean insideParens = false;
		int start = 0;
		for (int i = 0; i < epaStateName.length(); i++) {

			if (epaStateName.charAt(i) == '(') {
				insideParens = true;
			}
			if (epaStateName.charAt(i) == ')') {
				insideParens = false;
			}
			if (epaStateName.charAt(i) == ',' && !insideParens) {
				final String name = epaStateName.substring(start, i).trim();
				start = i + 1;

				if (!name.isEmpty()) {
					actionNames.add(name);
				}
			}
		}

		final String name = epaStateName.substring(start).trim();
		if (!name.isEmpty()) {
			actionNames.add(name);
		}

		return actionNames;
	}
	
}
