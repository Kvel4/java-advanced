package info.kgeorgiy.ja.monakhov.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {
    private static final Comparator<Student> NAME_ORDER = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparing(Student::getId);

    private static final Comparator<Student> ID_ORDER = Student::compareTo;

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mappedList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mappedList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return mappedList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mappedList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return mappedCollection(students, Student::getFirstName, TreeSet::new);
    }


    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream().max(ID_ORDER).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortedList(students, ID_ORDER);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortedList(students, NAME_ORDER);
    }


    // :NOTE: Дублирование
    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return filteredAndSortedList(students, lambdaTemplate(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return filteredAndSortedList(students, lambdaTemplate(Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return filteredAndSortedList(students, lambdaTemplate(Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return students.stream()
                .filter(g -> g.getGroup().equals(group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private <T> Predicate<? super Student> lambdaTemplate(Function<Student, T> filter, T t) {
        return student -> filter.apply(student).equals(t);
    }

    private static <T, C extends Collection<T>> C mappedCollection(final Collection<Student> students, final Function<Student, T> mapper, final Supplier<C> collector) {
        return students.stream().map(mapper).collect(Collectors.toCollection(collector));
    }

    private static <T> List<T> mappedList(final Collection<Student> students, final Function<Student, T> mapper) {
        return mappedCollection(students, mapper, ArrayList::new);
    }

    private static List<Student> sortedList(final Collection<Student> students, final Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private static List<Student> filteredAndSortedList(final Collection<Student> students, final Predicate<? super Student> filter) {
        return students.stream().filter(filter).sorted(NAME_ORDER).collect(Collectors.toList());
    }
}
