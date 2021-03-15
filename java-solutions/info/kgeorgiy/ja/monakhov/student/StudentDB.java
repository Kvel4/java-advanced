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

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mappedCollection(students, Student::getFirstName, ArrayList::new);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mappedCollection(students, Student::getLastName, ArrayList::new);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mappedCollection(students, Student::getGroup, ArrayList::new);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mappedCollection(students, student -> student.getFirstName() + " " + student.getLastName(), ArrayList::new);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappedCollection(students, Student::getFirstName, TreeSet::new);
    }


    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedList(students, NAME_ORDER);
    }


    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filteredAndSortedList(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filteredAndSortedList(students, student -> student.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filteredAndSortedList(students, student -> student.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(g -> g.getGroup().equals(group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private <T, C extends Collection<T>> C mappedCollection(Collection<Student> students, Function<Student, T> mapper, Supplier<C> collector) {
        return students.stream().map(mapper).collect(Collectors.toCollection(collector));
    }

    private List<Student> sortedList(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<Student> filteredAndSortedList(Collection<Student> students, Predicate<? super Student> filter) {
        return students.stream().filter(filter).sorted(NAME_ORDER).collect(Collectors.toList());
    }
}
