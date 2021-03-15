package info.kgeorgiy.ja.monakhov.arrayset;

import java.util.*;
import java.util.SortedSet;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> list;
    private Comparator<? super E> comparator;

    public ArraySet(){
        list = new ArrayList<>();
    }

    public ArraySet(Collection<? extends E> collection) {
        list = new ArrayList<>(new TreeSet<>(collection));
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        TreeSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        this.comparator = comparator;
        list = new ArrayList<>(treeSet);
    }

    private ArraySet(Comparator<? super E> comparator, List<E> view) {
        list = view;
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public ArraySet<E> subSet(E fromElement, E toElement) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("From index must be lesser or equal than to index");
        }
        return subSet(find(fromElement), find(toElement));
    }


    @Override
    public ArraySet<E> headSet(E toElement) {
        return subSet(0, find(toElement));
    }

    @Override
    public ArraySet<E> tailSet(E fromElement) {
        return subSet(find(fromElement), list.size());
    }

    @Override
    public E first() {
        checkNonEmpty();
        return list.get(0);
    }

    @Override
    public E last() {
        checkNonEmpty();
        return list.get(list.size() - 1);
    }

    @Override
    public boolean contains(Object o) {
        return binarySearch((E) o) >= 0;
    }

    private void checkNonEmpty() {
        if (list.isEmpty()) throw new NoSuchElementException("Set is empty");
    }

    private int find(E element) {
        int i = binarySearch(element);
        return i >= 0 ? i : -i - 1;
    }

    private int binarySearch(E element) {
        return Collections.binarySearch(list, Objects.requireNonNull(element), comparator);
    }

    private ArraySet<E> subSet(int from, int to) {
        return new ArraySet<>(comparator, list.subList(from, to));
    }
}
