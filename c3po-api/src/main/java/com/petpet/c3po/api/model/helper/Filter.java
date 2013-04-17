package com.petpet.c3po.api.model.helper;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.petpet.c3po.api.dao.PersistenceLayer;

/**
 * This class is used to define a filter for a c3po collection. It has a list of
 * filter conditions that should be applied on the data before executing a
 * query. This allows the drill down into the data.
 * 
 * The persistence layer provider should interpret the conditions by logically
 * concatenating all conditions with an AND. If two or more filter conditions
 * apply to the same property, then they should be applied with a logical OR.
 * Consider the following example where we want to filter all objects from a
 * collection 'A' that have either text/html or text/xml mimetype. In such a
 * case the filter that will be passed to the {@link PersistenceLayer} will have
 * three {@link FilterCondition} objects:
 * 
 * first - property is 'collection' and value is 'A' <br>
 * second - property is 'mimetype' and value is 'text/html' <br>
 * third - property is 'mimetype' and value is 'text/xml'
 * 
 * Consider now that the same filter hast to be applied but it has go over both
 * collection 'A' and collection 'B'. The caller has to include an additional
 * {@link FilterCondition} where the property is 'collection' and the value is
 * 'B' and the interpreter of this filter should use a logical OR for the
 * collection property as well.
 * 
 * @author Petar Petrov <me@petarpetrov.org>
 * 
 */
public class Filter {

  /**
   * A list of filter conditions, that has to be interpreted by the persistence
   * provider to a query or addition to a query.
   */
  private List<FilterCondition> conditions;

  @Deprecated
  private String descriminator;

  /**
   * The collection that is filtered.
   */
  @Deprecated
  private String collection;

  /**
   * The property that is filtered by this filter (e.g. mimetype).
   */
  @Deprecated
  private String property;

  /**
   * The value of the property by which the filter is partitioning (e.g.
   * application/pdf).
   */
  @Deprecated
  private String value;

  /**
   * Create a new empty filter with an initialized empty list of
   * {@link FilterCondition}s.
   */
  public Filter() {
    conditions = new ArrayList<FilterCondition>();
  }

  /**
   * Creates a new filter and sets the {@link FilterCondition}s to the passed
   * list.
   * 
   * @param fcs
   *          the filter conditions to use.
   */
  public Filter(List<FilterCondition> fcs) {
    conditions = fcs;
  }

  public List<FilterCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<FilterCondition> conditions) {
    this.conditions = conditions;
  }

  /**
   * Adds a new filter condition unless it is null or it already exists.
   * 
   * @param fc
   *          the filter condition to add.
   */
  public void addFilterCondition(FilterCondition fc) {
    if (conditions == null) {
      conditions = new ArrayList<FilterCondition>();
    }

    if (fc != null && !conditions.contains(fc))
      conditions.add(fc);
  }

  /**
   * Creates a default root filter. This means that the filter has no parent
   * filter.
   * 
   * @param collection
   *          the collection to filter.
   * @param property
   *          the property to apply.
   * @param value
   *          the value of the property to apply for this filter.
   */
  @Deprecated
  public Filter(String collection, String property, String value) {
    this.collection = collection;
    this.property = property;
    this.value = value;
  }

  @Deprecated
  public String getDescriminator() {
    return descriminator;
  }

  @Deprecated
  public void setDescriminator(String id) {
    this.descriminator = id;
  }

  @Deprecated
  public String getCollection() {
    return collection;
  }

  @Deprecated
  public void setCollection(String collection) {
    this.collection = collection;
  }

  @Deprecated
  public String getProperty() {
    return property;
  }

  @Deprecated
  public void setProperty(String property) {
    this.property = property;
  }

  @Deprecated
  public String getValue() {
    return value;
  }

  @Deprecated
  public void setValue(String value) {
    this.value = value;
  }

  @Deprecated
  public DBObject getDocument() {
    final BasicDBObject filter = new BasicDBObject();
    filter.put("descriminator", this.descriminator);
    filter.put("collection", collection);
    filter.put("property", property);
    filter.put("value", value);

    return filter;
  }
}