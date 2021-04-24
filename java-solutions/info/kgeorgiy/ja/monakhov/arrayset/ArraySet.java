package info.kgeorgiy.ja.monakhov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> list;
    private Comparator<? super E> comparator;

    public ArraySet(){
        list = new ArrayList<>();
    }

    public ArraySet(final Collection<? extends E> collection) {
        list = new ArrayList<>(new TreeSet<>(collection));
    }

    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        final TreeSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        this.comparator = comparator;
        list = new ArrayList<>(treeSet);
    }

    private ArraySet(final Comparator<? super E> comparator, final List<E> view) {
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
    public ArraySet<E> subSet(final E fromElement, final E toElement) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("From index must be lesser or equal than to index");
        }
        return subSet(find(fromElement), find(toElement));
    }


    @Override
    public ArraySet<E> headSet(final E toElement) {
        return subSet(0, find(toElement));
    }

    @Override
    public ArraySet<E> tailSet(final E fromElement) {
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
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return binarySearch((E) o) >= 0;
    }

    private void checkNonEmpty() {
        if (list.isEmpty()) throw new NoSuchElementException("Set is empty");
    }

    private int find(final E element) {
        final int i = binarySearch(element);
        return i >= 0 ? i : -i - 1;
    }

    private int binarySearch(final E element) {
        return Collections.binarySearch(list, Objects.requireNonNull(element), comparator);
    }

    private ArraySet<E> subSet(final int from, final int to) {
        return new ArraySet<>(comparator, list.subList(from, to));
    }
}
