package io.prometheus.metrics.model.snapshots;

import static io.prometheus.metrics.model.snapshots.PrometheusNaming.isValidLabelName;
import static io.prometheus.metrics.model.snapshots.PrometheusNaming.prometheusName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/** Immutable set of name/value pairs, sorted by name. */
public final class Labels implements Comparable<Labels>, Iterable<Label> {

  public static final Labels EMPTY;

  static {
    String[] nameArray = new String[] {};
    String[] valueArray = new String[] {};
    EMPTY = new Labels(nameArray, nameArray, valueArray);
  }

  // prometheusNames is the same as names, but dots are replaced with underscores.
  // Labels is sorted by prometheusNames.
  // If names[i] does not contain a dot, prometheusNames[i] references the same String as names[i]
  // so that we don't have unnecessary duplicates of strings.
  // If none of the names contains a dot, then prometheusNames references the same array as names
  // so that we don't have unnecessary duplicate arrays.
  private final String[] prometheusNames;
  private final String[] names;
  private final String[] values;

  private Labels(String[] names, String[] prometheusNames, String[] values) {
    this.names = names;
    this.prometheusNames = prometheusNames;
    this.values = values;
  }

  @SuppressWarnings("ReferenceEquality")
  public boolean isEmpty() {
    return this == EMPTY || this.equals(EMPTY);
  }

  /**
   * Create a new Labels instance. You can either create Labels with one of the static {@code
   * Labels.of(...)} methods, or you can use the {@link Labels#builder()}.
   *
   * @param keyValuePairs as in {@code {name1, value1, name2, value2}}. Length must be even. {@link
   *     PrometheusNaming#isValidLabelName(String)} must be true for each name. Use {@link
   *     PrometheusNaming#sanitizeLabelName(String)} to convert arbitrary strings to valid label
   *     names. Label names must be unique (no duplicate label names).
   */
  public static Labels of(String... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("Key/value pairs must have an even length");
    }
    if (keyValuePairs.length == 0) {
      return EMPTY;
    }
    String[] nameList = new String[keyValuePairs.length / 2];
    String[] valueList = new String[keyValuePairs.length / 2];
    for (int i = 0; 2 * i < keyValuePairs.length; i++) {
      nameList[i] = keyValuePairs[2 * i];
      valueList[i] = keyValuePairs[2 * i + 1];
    }
    String[] prometheusNamesLocal = makePrometheusNames(nameList);
    sortAndValidate(nameList, prometheusNamesLocal, valueList);
    return new Labels(nameList, prometheusNamesLocal, valueList);
  }

  // package private for testing
  /**
   * Create a new Labels instance. You can either create Labels with one of the static {@code
   * Labels.of(...)} methods, or you can use the {@link Labels#builder()}.
   *
   * @param nameList label names. {@link PrometheusNaming#isValidLabelName(String)} must be true for
   *     each name. Use {@link PrometheusNaming#sanitizeLabelName(String)} to convert arbitrary
   *     strings to valid label names. Label names must be unique (no duplicate label names).
   * @param valueList label values. {@code nameList.size()} must be equal to {@code
   *     valueList.size()}.
   */
  public static Labels of(List<String> nameList, List<String> valueList) {
    if (nameList.size() != valueList.size()) {
      throw new IllegalArgumentException("Names and values must have the same size.");
    }
    if (nameList.isEmpty()) {
      return EMPTY;
    }
    String[] namesCopy = nameList.toArray(new String[0]);
    String[] valuesCopy = valueList.toArray(new String[0]);
    String[] prometheusNamesLocal = makePrometheusNames(namesCopy);
    sortAndValidate(namesCopy, prometheusNamesLocal, valuesCopy);
    return new Labels(namesCopy, prometheusNamesLocal, valuesCopy);
  }

  /**
   * Create a new Labels instance. You can either create Labels with one of the static {@code
   * Labels.of(...)} methods, or you can use the {@link Labels#builder()}.
   *
   * @param nameArray label names. {@link PrometheusNaming#isValidLabelName(String)} must be true
   *     for each name. Use {@link PrometheusNaming#sanitizeLabelName(String)} to convert arbitrary
   *     strings to valid label names. Label names must be unique (no duplicate label names).
   * @param valueArray label values. {@code nameArray.length} must be equal to {@code
   *     valueArray.length}.
   */
  public static Labels of(String[] nameArray, String[] valueArray) {
    if (nameArray.length != valueArray.length) {
      throw new IllegalArgumentException("Names and values must have the same length.");
    }
    if (nameArray.length == 0) {
      return EMPTY;
    }
    String[] namesCopy = Arrays.copyOf(nameArray, nameArray.length);
    String[] valuesCopy = Arrays.copyOf(valueArray, valueArray.length);
    String[] prometheusNamesLocal = makePrometheusNames(namesCopy);
    sortAndValidate(namesCopy, prometheusNamesLocal, valuesCopy);
    return new Labels(namesCopy, prometheusNamesLocal, valuesCopy);
  }

  static String[] makePrometheusNames(String[] inputNames) {
    String[] prometheusNamesArray = inputNames;
    for (int i = 0; i < inputNames.length; i++) {
      if (inputNames[i].contains(".")) {
        if (prometheusNamesArray == inputNames) {
          prometheusNamesArray = Arrays.copyOf(inputNames, inputNames.length);
        }
        prometheusNamesArray[i] = PrometheusNaming.prometheusName(inputNames[i]);
      }
    }
    return prometheusNamesArray;
  }

  /**
   * Test if these labels contain a specific label name.
   *
   * <p>Dots are treated as underscores, so {@code contains("my.label")} and {@code
   * contains("my_label")} are the same.
   */
  public boolean contains(String labelName) {
    return get(labelName) != null;
  }

  /**
   * Get the label value for a given label name.
   *
   * <p>Returns {@code null} if the {@code labelName} is not found.
   *
   * <p>Dots are treated as underscores, so {@code get("my.label")} and {@code get("my_label")} are
   * the same.
   */
  public String get(String labelName) {
    labelName = prometheusName(labelName);
    for (int i = 0; i < prometheusNames.length; i++) {
      if (prometheusNames[i].equals(labelName)) {
        return values[i];
      }
    }
    return null;
  }

  private static void sortAndValidate(String[] names, String[] prometheusNames, String[] values) {
    sort(names, prometheusNames, values);
    validateNames(names, prometheusNames);
  }

  private static void validateNames(String[] names, String[] prometheusNames) {
    for (int i = 0; i < names.length; i++) {
      if (!isValidLabelName(names[i])) {
        throw new IllegalArgumentException("'" + names[i] + "' is an illegal label name");
      }
      // The arrays are sorted, so duplicates are next to each other
      if (i > 0 && prometheusNames[i - 1].equals(prometheusNames[i])) {
        throw new IllegalArgumentException(names[i] + ": duplicate label name");
      }
    }
  }

  private static void sort(String[] names, String[] prometheusNames, String[] values) {
    // bubblesort
    int n = prometheusNames.length;
    for (int i = 0; i < n - 1; i++) {
      for (int j = 0; j < n - i - 1; j++) {
        if (prometheusNames[j].compareTo(prometheusNames[j + 1]) > 0) {
          swap(j, j + 1, names, prometheusNames, values);
        }
      }
    }
  }

  private static void swap(
      int i, int j, String[] names, String[] prometheusNames, String[] values) {
    String tmp = names[j];
    names[j] = names[i];
    names[i] = tmp;
    tmp = values[j];
    values[j] = values[i];
    values[i] = tmp;
    if (prometheusNames != names) {
      tmp = prometheusNames[j];
      prometheusNames[j] = prometheusNames[i];
      prometheusNames[i] = tmp;
    }
  }

  @Override
  public Iterator<Label> iterator() {
    return asList().iterator();
  }

  public Stream<Label> stream() {
    return asList().stream();
  }

  public int size() {
    return names.length;
  }

  public String getName(int i) {
    return names[i];
  }

  /**
   * Like {@link #getName(int)}, but dots are replaced with underscores.
   *
   * <p>This is used by Prometheus exposition formats.
   */
  public String getPrometheusName(int i) {
    return prometheusNames[i];
  }

  public String getValue(int i) {
    return values[i];
  }

  /**
   * Create a new Labels instance containing the labels of this and the labels of other. This and
   * other must not contain the same label name.
   */
  public Labels merge(Labels other) {
    if (this.isEmpty()) {
      return other;
    }
    if (other.isEmpty()) {
      return this;
    }
    String[] nameArray = new String[this.names.length + other.names.length];
    String[] prometheusNamesArray = nameArray;
    if (this.names != this.prometheusNames || other.names != other.prometheusNames) {
      prometheusNamesArray = new String[nameArray.length];
    }
    String[] valueArray = new String[nameArray.length];
    int thisPos = 0;
    int otherPos = 0;

    // Merge the two sorted arrays
    while (thisPos < this.names.length && otherPos < other.names.length) {
      int cmp = this.prometheusNames[thisPos].compareTo(other.prometheusNames[otherPos]);
      if (cmp < 0) {
        nameArray[thisPos + otherPos] = this.names[thisPos];
        valueArray[thisPos + otherPos] = this.values[thisPos];
        if (prometheusNamesArray != nameArray) {
          prometheusNamesArray[thisPos + otherPos] = this.prometheusNames[thisPos];
        }
        thisPos++;
      } else if (cmp > 0) {
        nameArray[thisPos + otherPos] = other.names[otherPos];
        valueArray[thisPos + otherPos] = other.values[otherPos];
        if (prometheusNamesArray != nameArray) {
          prometheusNamesArray[thisPos + otherPos] = other.prometheusNames[otherPos];
        }
        otherPos++;
      } else {
        throw new IllegalArgumentException("Duplicate label name: '" + this.names[thisPos] + "'.");
      }
    }

    // Add remaining elements from this
    while (thisPos < this.names.length) {
      nameArray[thisPos + otherPos] = this.names[thisPos];
      valueArray[thisPos + otherPos] = this.values[thisPos];
      if (prometheusNamesArray != nameArray) {
        prometheusNamesArray[thisPos + otherPos] = this.prometheusNames[thisPos];
      }
      thisPos++;
    }

    // Add remaining elements from other
    while (otherPos < other.names.length) {
      nameArray[thisPos + otherPos] = other.names[otherPos];
      valueArray[thisPos + otherPos] = other.values[otherPos];
      if (prometheusNamesArray != nameArray) {
        prometheusNamesArray[thisPos + otherPos] = other.prometheusNames[otherPos];
      }
      otherPos++;
    }

    return new Labels(nameArray, prometheusNamesArray, valueArray);
  }

  /**
   * Create a new Labels instance containing the labels of this and the labels passed as names and
   * values. The new label names must not already be contained in this Labels instance.
   */
  public Labels merge(String[] nameArray, String[] valueArray) {
    if (this.equals(EMPTY)) {
      return Labels.of(nameArray, valueArray);
    }
    String[] mergedNames = new String[this.names.length + nameArray.length];
    String[] mergedValues = new String[this.values.length + valueArray.length];
    System.arraycopy(this.names, 0, mergedNames, 0, this.names.length);
    System.arraycopy(this.values, 0, mergedValues, 0, this.values.length);
    System.arraycopy(nameArray, 0, mergedNames, this.names.length, nameArray.length);
    System.arraycopy(valueArray, 0, mergedValues, this.values.length, valueArray.length);
    String[] prometheusNames = makePrometheusNames(mergedNames);
    sortAndValidate(mergedNames, prometheusNames, mergedValues);
    return new Labels(mergedNames, prometheusNames, mergedValues);
  }

  /**
   * Create a new Labels instance containing the labels of this and the label passed as name and
   * value. The label name must not already be contained in this Labels instance.
   */
  public Labels add(String name, String value) {
    return merge(Labels.of(name, value));
  }

  public boolean hasSameNames(Labels other) {
    return Arrays.equals(prometheusNames, other.prometheusNames);
  }

  public boolean hasSameValues(Labels other) {
    return Arrays.equals(values, other.values);
  }

  @Override
  public int compareTo(Labels other) {
    int result = compare(prometheusNames, other.prometheusNames);
    if (result != 0) {
      return result;
    }
    return compare(values, other.values);
  }

  // Looks like Java doesn't have a compareTo() method for arrays.
  private int compare(String[] array1, String[] array2) {
    int result;
    for (int i = 0; i < array1.length; i++) {
      if (array2.length <= i) {
        return 1;
      }
      result = array1[i].compareTo(array2[i]);
      if (result != 0) {
        return result;
      }
    }
    if (array2.length > array1.length) {
      return -1;
    }
    return 0;
  }

  private List<Label> asList() {
    List<Label> result = new ArrayList<>(names.length);
    for (int i = 0; i < names.length; i++) {
      result.add(new Label(names[i], values[i]));
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * This must not be used in Prometheus exposition formats because names may contain dots.
   *
   * <p>However, for debugging it's better to show the original names rather than the Prometheus
   * names.
   */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("{");
    for (int i = 0; i < names.length; i++) {
      if (i > 0) {
        b.append(",");
      }
      b.append(names[i]);
      b.append("=\"");
      appendEscapedLabelValue(b, values[i]);
      b.append("\"");
    }
    b.append("}");
    return b.toString();
  }

  private void appendEscapedLabelValue(StringBuilder b, String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\':
          b.append("\\\\");
          break;
        case '\"':
          b.append("\\\"");
          break;
        case '\n':
          b.append("\\n");
          break;
        default:
          b.append(c);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Labels labels = (Labels) o;
    return labels.hasSameNames(this) && labels.hasSameValues(this);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(prometheusNames);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<String> names = new ArrayList<>();
    private final List<String> values = new ArrayList<>();

    private Builder() {}

    /** Add a label. Call multiple times to add multiple labels. */
    public Builder label(String name, String value) {
      names.add(name);
      values.add(value);
      return this;
    }

    public Labels build() {
      return Labels.of(names, values);
    }
  }
}
