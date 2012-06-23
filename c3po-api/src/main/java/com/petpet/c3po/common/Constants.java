package com.petpet.c3po.common;

public final class Constants {

  /**
   * The url for the xml schema property used by the sax parser while validating
   * xml files against their schemata.
   */
  public static final String XML_SCHEMA_PROPERTY = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  /**
   * The url for the xml schema language used by the sax parser while validating
   * xml files against their schemata.
   */
  public static final String XML_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

  /**
   * The version of the generated profile.
   */
  public static final String PROFILE_FORMAT_VERSION = "0.1";

  /**
   * The elements collection in the document store.
   */
  public static final String TBL_ELEMENTS = "elements";

  /**
   * The properties collection in the document store.
   */
  public static final String TBL_PROEPRTIES = "properties";

  /**
   * The source collection in the document store.
   */
  public static final String TBL_SOURCES = "sources";

  /**
   * A c3po configuration for the collection on which to operate.
   */
  public static final String CNF_COLLECTION_NAME = "c3po.collection.name";

  /**
   * A c3po configuration for the location where the metadata is.
   */
  public static final String CNF_COLLECTION_LOCATION = "c3po.collection.location";

  /**
   * Experimental property allowing to infer the date of the objects, if their
   * file names have a specific format.
   */
  public static final String CNF_INFER_DATE = "adaptor.inference.date";

  /**
   * A collection identifier configuration for the adaptors.s
   */
  public static final String CNF_COLLECTION_ID = "adaptor.collection.identifier";

  /**
   * A configuartion for recursive processing.
   */
  public static final String CNF_RECURSIVE = "c3po.recursive";

  /**
   * The thread count configuration during meta data harvesting.
   */
  public static final String CNF_THREAD_COUNT = "c3po.thread.count";

  /**
   * The hostname of the server where the db is running.
   */
  public static final String CNF_DB_HOST = "db.host";

  /**
   * The port of the server where the db is listening to.
   */
  public static final String CBF_DB_PORT = "db.port";

  /**
   * The database name.
   */
  public static final String CNF_DB_NAME = "db.name";

  /**
   * A javascript Map function for building a histogram of a specific property.
   * All occurrences of that property are used (if they do not have conflcited
   * values). Note that there is a '{}' wildcard that has to be replaced with
   * the id of the desired property, prior to usage.
   */
  public static final String HISTOGRAM_MAP = "function map() {if (this.metadata['{}'] != null && this.metadata['{}'].status !== 'CONFLICT') {emit(this.metadata['{}'].value, 1);} else {emit('Unknown', 1)}}";

  /**
   * The reduce function for the {@link Constants#HISTOGRAM_MAP}.
   */
  public static final String HISTOGRAM_REDUCE = "function reduce(key, values) {var res = 0;values.forEach(function (v) {res += v;});return res;}";

  /**
   * A javascript Map function for calculating the min, max, sum, avg, sd and
   * var of a numeric property. Note that there is a wildcard {1} that has to be
   * replaced with the id of the desired numeric property prior to usage.
   */
  public static final String AGGREGATE_MAP = "function map() {emit(1,{sum: this.metadata['{1}'].value, min: this.metadata['{1}'].value,max: this.metadata['{1}'].value,count:1,diff: 0,});}";

  /**
   * The same as {@link Constants#AGGREGATE_MAP} but it aggregates the desired
   * property only for elements where the passed filter has a specific value.
   * {1} - the filter property id (e.g. 'mimetype') {2} - the value of the
   * filter (e.g. 'application/pdf') {3} - the property to aggregate (e.g.
   * 'size')
   */
  public static final String FILTER_AGGREGATE_MAP = "function map() {if (this.metadata['{1}'].value === '{2}') {emit(1,{sum: this.metadata['{3}'].value, min: this.metadata['{3}'].value,max: this.metadata['{3}'].value,count:1,diff: 0,});}}";

  /**
   * The reduce of the aggregation functions.
   */
  public static final String AGGREGATE_REDUCE = "function reduce(key, values) {var a = values[0];for (var i=1; i < values.length; i++){var b = values[i];var delta = a.sum/a.count - b.sum/b.count;var weight = (a.count * b.count)/(a.count + b.count);a.diff += b.diff + delta*delta*weight;a.sum += b.sum;a.count += b.count;a.min = Math.min(a.min, b.min);a.max = Math.max(a.max, b.max);}return a;}";

  /**
   * A finalize function for the aggregation map reduce job, to calculate the
   * average, standard deviation and variance.
   */
  public static final String AGGREGATE_FINALIZE = "function finalize(key, value){ value.avg = value.sum / value.count;value.variance = value.diff / value.count;value.stddev = Math.sqrt(value.variance);return value;}";

  // "function reduce(key, values) {var res = {count: 0}; values.forEach(function (v) {res.count += v.count}); return res;}";

  private Constants() {

  }
}
