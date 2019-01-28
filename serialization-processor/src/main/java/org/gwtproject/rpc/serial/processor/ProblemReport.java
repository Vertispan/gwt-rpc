package org.gwtproject.rpc.serial.processor;

import com.squareup.javapoet.ClassName;

import javax.annotation.processing.Messager;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.util.*;

/**
 * A collection of reported problems; these are accumulated during the
 * SerializableTypeOracleBuilder's isSerializable analysis, and what to do about
 * the problems is decided only later.
 */
public class ProblemReport {

    /**
     * Priority of problems. {@link #FATAL} problems will fail a build that would
     * otherwise have succeeded, for example because of a bad custom serializer
     * used only as a subclass of a superclass with other viable subtypes.
     * {@link #DEFAULT} problems might or might not be fatal, depending on overall
     * results accumulated later. {@link #AUXILIARY} problems are not fatal, and
     * often not even problems by themselves, but diagnostics related to default
     * problems (e.g. type filtration, which might suppress an
     * intended-to-serialize class).
     */
    public enum Priority {
        FATAL, DEFAULT, AUXILIARY
    }

    /**
     * An individual report, which may require multiple entries (expressed as logs
     * under a branchpoint), but relates to an individual issue.
     */
    public static class Problem {
        private String message;
        private List<String> childMessages;

        private Problem(String message, String[] children) {
            this.message = message;
            // most problems don't have sub-messages, so init at zero size
            childMessages = new ArrayList<String>(children.length);
            for (int i = 0; i < children.length; i++) {
                childMessages.add(children[i]);
            }
        }

        public void addChild(String message) {
            childMessages.add(message);
        }

        public String getPrimaryMessage() {
            return message;
        }

        public Iterable<String> getSubMessages() {
            return childMessages;
        }

        public boolean hasSubMessages() {
            return !childMessages.isEmpty();
        }
    }

    private Map<TypeMirror, List<Problem>> allProblems;
    private Map<TypeMirror, List<Problem>> auxiliaries;
    private Map<TypeMirror, List<Problem>> fatalProblems;
    private TypeMirror contextType;

    private final Messager messager;

    /**
     * Creates a new, empty, context-less ProblemReport.
     * @param messager
     */
    public ProblemReport(Messager messager) {
        this.messager = messager;
        Comparator<TypeMirror> comparator = new Comparator<TypeMirror>() {
            public int compare(TypeMirror o1, TypeMirror o2) {
                assert o1 != null;
                assert o2 != null;
                return ClassName.get(o1).toString().compareTo(
                        ClassName.get(o2).toString());
            }
        };
        allProblems = new TreeMap<TypeMirror, List<Problem>>(comparator);
        auxiliaries = new TreeMap<TypeMirror, List<Problem>>(comparator);
        fatalProblems = new TreeMap<TypeMirror, List<Problem>>(comparator);
        contextType = null;
    }

    /**
     * Adds a problem for a given type. This also sorts the problems into
     * collections by priority.
     *
     * @param type the problematic type
     * @param message the description of the problem
     * @param priority priority of the problem.
     * @param extraLines additional continuation lines for the message, usually
     *          for additional explanations.
     */
    public Problem add(TypeMirror type, String message, Priority priority, String... extraLines) {
        String contextString = "";
        if (contextType != null) {
            contextString = " (reached via " + ClassName.get(contextType) + ")";
        }
        message = message + contextString;
        Problem entry = new Problem(message, extraLines);
        if (priority == Priority.AUXILIARY) {
            addToMap(type, entry, auxiliaries);
            return entry;
        }

        // both FATAL and DEFAULT problems go in allProblems...
        addToMap(type, entry, allProblems);

        // only FATAL problems go in fatalProblems...
        if (priority == Priority.FATAL) {
            addToMap(type, entry, fatalProblems);
        }
        return entry;
    }

    public String getWorstMessageForType(TypeMirror type) {
        List<Problem> list = fatalProblems.get(type);
        if (list == null) {
            list = allProblems.get(type);
            if (list == null) {
                list = auxiliaries.get(type);
            }
        }
        if (list == null) {
            return null;
        }
        return list.get(0).getPrimaryMessage() + (list.size() > 1 ? ", etc." : "");
    }

    /**
     * Were any problems reported as "fatal"?
     */
    public boolean hasFatalProblems() {
        return !fatalProblems.isEmpty();
    }

    /**
     * Reports all problems to the logger supplied, at the log level supplied. The
     * problems are assured of being reported in lexographic order of type names.
     *
     * //@param logger logger to receive problem reports
     * @param problemLevel severity level at which to report problems.
     * @param auxLevel severity level at which to report any auxiliary messages.
     */
    public void report(/*TreeLogger logger, */Kind problemLevel, Kind auxLevel) {
        doReport(/*logger, */auxLevel, auxiliaries);
        doReport(/*logger, */problemLevel, allProblems);
    }

    /**
     * Reports only urgent problems to the logger supplied, at the log level
     * supplied. The problems are assured of being reported in lexographic order
     * of type names.
     *
     * //@param logger logger to receive problem reports
     * @param level severity level at which to report problems.
     */
    public void reportFatalProblems(/*TreeLogger logger, */Kind level) {
        doReport(/*logger, */level, fatalProblems);
    }

    /**
     * Sets the context type currently being analyzed. Problems found will include
     * reference to this context, until reset with another call to this method.
     * Context may be canceled with a {@code null} value here.
     *
     * @param newContext the type under analysis
     */
    public void setContextType(TypeMirror newContext) {
        contextType = newContext;
    }

    /**
     * Test accessor returning list of auxiliary "problems" logged against a given
     * type.
     *
     * @param type type to fetch problems for
     * @return {@code null} if no auxiliaries were logged. Otherwise, a list of
     *         strings describing messages, including the context in which the
     *         problem was found.
     */
    List<Problem> getAuxiliaryMessagesForType(TypeMirror type) {
        List<Problem> list = auxiliaries.get(type);
        if (list == null) {
            list = new ArrayList<Problem>(0);
        }
        return list;
    }

    /**
     * Test accessor returning list of problems logged against a given type.
     *
     * @param type type to fetch problems for
     * @return {@code null} if no problems were logged. Otherwise, a list of
     *         strings describing problems, including the context in which the
     *         problem was found.
     */
    List<Problem> getProblemsForType(TypeMirror type) {
        List<Problem> list = allProblems.get(type);
        if (list == null) {
            list = new ArrayList<Problem>(0);
        }
        return list;
    }

    /**
     * Adds an entry to one of the problem maps.
     *
     * @param type the type to add
     * @param problem the message to add for {@code type}
     * @param map the map to add to
     */
    private void addToMap(TypeMirror type, Problem problem, Map<TypeMirror, List<Problem>> map) {
        List<Problem> list = map.get(type);
        if (list == null) {
            list = new ArrayList<Problem>();
            map.put(type, list);
        }
        list.add(problem);
    }

    /**
     * Logs all of the problems from one of the problem maps.
     *
     * @param level the level for messages
     * @param problems the problems to log
     */
    private void doReport(/*TreeLogger logger, */Kind level, Map<TypeMirror, List<Problem>> problems) {
        for (List<Problem> problemList : problems.values()) {
            for (Problem problem : problemList) {
                messager.printMessage(level, problem.getPrimaryMessage());
                if (problem.hasSubMessages()) {
                    for (String sub : problem.getSubMessages()) {
                        messager.printMessage(level, sub);
                    }
                }
            }
        }
//        if (!logger.isLoggable(level)) {
//            return;
//        }
//        for (List<Problem> problemList : problems.values()) {
//            for (Problem problem : problemList) {
//                if (problem.hasSubMessages()) {
//                    TreeLogger sublogger = logger.branch(level, problem.getPrimaryMessage());
//                    for (String sub : problem.getSubMessages()) {
//                        sublogger.log(level, sub);
//                    }
//                } else {
//                    logger.log(level, problem.getPrimaryMessage());
//                }
//            }
//        }
    }
}
